package com.lionlink.backend.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.lionlink.backend.domain.chat.dto.request.ChatMessageSendRequest;
import com.lionlink.backend.domain.chat.dto.request.ChatRequestCreateRequest;
import com.lionlink.backend.domain.chat.dto.response.ChatMessageResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatRoomDetailResponse;
import com.lionlink.backend.domain.chat.entity.ChatRoom;
import com.lionlink.backend.domain.chat.entity.ChatRoomStatus;
import com.lionlink.backend.domain.chat.repository.ChatRoomRepository;
import com.lionlink.backend.domain.chat.service.ChatRequestService;
import com.lionlink.backend.domain.chat.service.ChatRoomService;
import com.lionlink.backend.domain.chat.service.MeetupChatRoomService;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatAcceptRequest;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatCreateRequest;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChat;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatMember;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatStatus;
import com.lionlink.backend.domain.coffeechat.entity.PreferredMode;
import com.lionlink.backend.domain.coffeechat.repository.CoffeeChatMemberRepository;
import com.lionlink.backend.domain.coffeechat.service.CoffeeChatService;
import com.lionlink.backend.domain.community.meetup.entity.Meetup;
import com.lionlink.backend.domain.community.meetup.repository.MeetupMemberRepository;
import com.lionlink.backend.domain.community.meetup.service.MeetupService;
import com.lionlink.backend.domain.community.post.service.PostService;
import com.lionlink.backend.domain.community.recruit.entity.CoffeeChatRecruit;
import com.lionlink.backend.domain.community.recruit.entity.RecruitStatus;
import com.lionlink.backend.domain.community.recruit.repository.RecruitMemberRepository;
import com.lionlink.backend.domain.community.recruit.service.RecruitService;
import com.lionlink.backend.domain.notification.entity.Notification;
import com.lionlink.backend.domain.notification.repository.NotificationRepository;
import com.lionlink.backend.domain.user.event.UserWithdrawnEvent;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.support.TestPosts;
import com.lionlink.backend.support.TestProfiles;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserWithdrawnCleanupTest {

	private static final String WITHDRAWN_COFFEE_CHAT = "함께하던 동문이 탈퇴해서 커피챗이 취소됐어요.";

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	@Autowired
	private CoffeeChatService coffeeChatService;

	@Autowired
	private PostService postService;

	@Autowired
	private MeetupService meetupService;

	@Autowired
	private RecruitService recruitService;

	@Autowired
	private ChatRequestService chatRequestService;

	@Autowired
	private ChatRoomService chatRoomService;

	@Autowired
	private ChatRoomRepository chatRoomRepository;

	@Autowired
	private CoffeeChatMemberRepository coffeeChatMemberRepository;

	@Autowired
	private MeetupMemberRepository meetupMemberRepository;

	@Autowired
	private RecruitMemberRepository recruitMemberRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private EntityManager entityManager;

	private Long me;
	private Long kim;
	private Long lee;
	private Long senior;

	@BeforeEach
	void setUp() {
		me = alumni("나사자");
		kim = alumni("김사자");
		lee = alumni("이사자");
		senior = alumni("선배사자");
		TestPosts.acceptGroupCoffeeChat(entityManager, senior);
		TestPosts.acceptGroupCoffeeChat(entityManager, lee);
	}

	@Test
	void 진행_중인_커피챗은_취소하고_함께_신청한_커피챗에서는_빠진다() {
		Long accepted = coffeeChatService.create(kim, request(senior, List.of())).id();
		coffeeChatService.accept(senior, accepted, new CoffeeChatAcceptRequest(List.of(LocalDateTime.now().plusDays(1)), null));
		Long group = coffeeChatService.create(me, request(lee, List.of(kim))).id();

		withdraw(kim);

		CoffeeChat canceled = entityManager.find(CoffeeChat.class, accepted);
		assertThat(canceled.getStatus()).isEqualTo(CoffeeChatStatus.CANCELED);
		assertThat(canceled.getCancelReason()).isEqualTo("회원 탈퇴");
		assertThat(notifications("COFFEE_CHAT_CANCELED")).extracting(Notification::getUserId, Notification::getActorId, Notification::getMessage)
				.containsExactly(tuple(senior, null, WITHDRAWN_COFFEE_CHAT));
		Long roomId = chatRoomRepository.findByCoffeeChatId(accepted).map(ChatRoom::getId).orElseThrow();
		assertThat(lastMessage(senior, roomId)).isEqualTo(WITHDRAWN_COFFEE_CHAT);

		assertThat(entityManager.find(CoffeeChat.class, group).getStatus()).isEqualTo(CoffeeChatStatus.PENDING);
		CoffeeChatMember member = coffeeChatMemberRepository.findByCoffeeChatIdAndUserId(group, kim).orElseThrow();
		assertThat(member.getLeftAt()).isNotNull();
	}

	@Test
	void 연_번개는_취소하고_참여한_번개에서는_빠진다() {
		Long hosted = postService.create(kim, TestPosts.meetup("김사자 번개", 4, null)).id();
		meetupService.join(lee, hosted);
		Long joined = postService.create(me, TestPosts.meetup("나사자 번개", 4, null)).id();
		meetupService.join(kim, joined);

		withdraw(kim);

		assertThat(entityManager.find(Meetup.class, hosted).isCanceled()).isTrue();
		assertThat(notifications("MEETUP_CANCELED")).extracting(Notification::getUserId).containsExactly(lee);
		Long hostedRoom = meetupRoom(hosted);
		assertThat(lastMessage(lee, hostedRoom)).isEqualTo(MeetupChatRoomService.CANCELED);
		assertThat(chatRoomService.getDetail(lee, hostedRoom).canSend()).isFalse();

		assertThat(meetupMemberRepository.findByMeetupPostIdOrderByJoinedAtAscUserIdAsc(joined)).extracting(m -> m.getUserId())
				.containsExactly(me);
		assertThat(lastMessage(me, meetupRoom(joined))).isEqualTo("탈퇴한 동문이 나갔어요.");
	}

	@Test
	void 연_모집은_취소하고_사자로_고른_모집은_마감하고_참여한_모집에서는_빠진다() {
		Long authored = postService.create(kim, TestPosts.recruit("김사자 모집", senior, 3)).id();
		recruitService.join(lee, authored);
		TestPosts.acceptGroupCoffeeChat(entityManager, kim);
		Long asSenior = postService.create(me, TestPosts.recruit("김사자에게 신청", kim, 3)).id();
		recruitService.join(lee, asSenior);
		Long joined = postService.create(me, TestPosts.recruit("선배사자에게 신청", senior, 3)).id();
		recruitService.join(kim, joined);

		withdraw(kim);

		assertThat(entityManager.find(CoffeeChatRecruit.class, authored).getStatus()).isEqualTo(RecruitStatus.ENDED);
		assertThat(notifications("RECRUIT_CANCELED")).extracting(Notification::getUserId).containsExactly(lee);
		assertThat(entityManager.find(CoffeeChatRecruit.class, asSenior).getStatus()).isEqualTo(RecruitStatus.ENDED);
		assertThat(notifications("RECRUIT_CLOSED")).extracting(Notification::getUserId, Notification::getActorId, Notification::getMessage)
				.containsExactlyInAnyOrder(
						tuple(me, null, "신청받을 사자가 탈퇴해서 모집이 마감됐어요. 김사자에게 신청"),
						tuple(lee, null, "신청받을 사자가 탈퇴해서 모집이 마감됐어요. 김사자에게 신청"));
		assertThat(entityManager.find(CoffeeChatRecruit.class, joined).getStatus()).isEqualTo(RecruitStatus.OPEN);
		assertThat(recruitMemberRepository.countByRecruitPostId(joined)).isEqualTo(1);
	}

	@Test
	void 대기_중인_메시지_요청은_취소하고_1대1_방은_남기되_보낼_수_없다() {
		Long active = chatRequestService.create(kim, new ChatRequestCreateRequest(me, "안녕하세요! 이야기 나눠요.")).roomId();
		chatRequestService.accept(me, active);
		Long requested = chatRequestService.create(lee, new ChatRequestCreateRequest(kim, "안녕하세요! 이야기 나눠요.")).roomId();

		withdraw(kim);

		assertThat(entityManager.find(ChatRoom.class, requested).getStatus()).isEqualTo(ChatRoomStatus.CANCELED);
		ChatRoomDetailResponse detail = chatRoomService.getDetail(me, active);
		assertThat(detail.members().get(0).isDeleted()).isTrue();
		assertThat(detail.canSend()).isFalse();
		assertThatThrownBy(() -> chatRoomService.send(me, active, new ChatMessageSendRequest("안녕하세요", null)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("탈퇴한 동문에게는 메시지를 보낼 수 없어요.");
	}

	private void withdraw(Long userId) {
		TestUsers.withdraw(entityManager, userId);
		eventPublisher.publishEvent(new UserWithdrawnEvent(userId));
		entityManager.flush();
		entityManager.clear();
	}

	private CoffeeChatCreateRequest request(Long receiverId, List<Long> memberIds) {
		return new CoffeeChatCreateRequest(receiverId, "ETC", "진로 고민", "선배님 이야기를 듣고 싶어요.", null, PreferredMode.ONLINE,
				null, memberIds, null);
	}

	private Long meetupRoom(Long postId) {
		return chatRoomRepository.findByMeetupId(postId).map(ChatRoom::getId).orElseThrow();
	}

	private String lastMessage(Long userId, Long roomId) {
		List<ChatMessageResponse> items = chatRoomService.getMessages(userId, roomId, null, null, 1).items();
		return items.get(items.size() - 1).content();
	}

	private List<Notification> notifications(String type) {
		return notificationRepository.findAll().stream().filter(notification -> notification.getType().equals(type)).toList();
	}

	private Long alumni(String name) {
		Long userId = TestUsers.approved(entityManager, name);
		TestProfiles.save(entityManager, TestProfiles.profile(userId));
		return userId;
	}
}
