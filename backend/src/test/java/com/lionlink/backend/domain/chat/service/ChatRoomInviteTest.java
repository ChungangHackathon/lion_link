package com.lionlink.backend.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionlink.backend.domain.chat.dto.request.ChatRequestCreateRequest;
import com.lionlink.backend.domain.chat.dto.request.ChatRoomInviteRequest;
import com.lionlink.backend.domain.chat.dto.response.ChatMessageResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatRoomInviteResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatUserResponse;
import com.lionlink.backend.domain.chat.entity.ChatRoom;
import com.lionlink.backend.domain.chat.exception.ChatErrorCode;
import com.lionlink.backend.domain.chat.repository.ChatRoomRepository;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatAcceptRequest;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatCreateRequest;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeSetting;
import com.lionlink.backend.domain.coffeechat.entity.MeetingMode;
import com.lionlink.backend.domain.coffeechat.entity.PreferredMode;
import com.lionlink.backend.domain.coffeechat.service.CoffeeChatService;
import com.lionlink.backend.domain.community.meetup.repository.MeetupMemberRepository;
import com.lionlink.backend.domain.community.post.service.PostService;
import com.lionlink.backend.domain.community.recruit.service.RecruitService;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.ErrorCode;
import com.lionlink.backend.support.TestPosts;
import com.lionlink.backend.support.TestProfiles;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ChatRoomInviteTest {

	@Autowired
	private ChatRoomService chatRoomService;

	@Autowired
	private ChatRequestService chatRequestService;

	@Autowired
	private CoffeeChatService coffeeChatService;

	@Autowired
	private PostService postService;

	@Autowired
	private RecruitService recruitService;

	@Autowired
	private ChatRoomRepository chatRoomRepository;

	@Autowired
	private MeetupMemberRepository meetupMemberRepository;

	@Autowired
	private EntityManager entityManager;

	private Long me;
	private Long kim;
	private Long lee;

	@BeforeEach
	void setUp() {
		me = alumni("나사자");
		kim = alumni("김사자");
		lee = alumni("이사자");
	}

	@Test
	void 번개방은_주최자가_초대하면_번개_참여자로도_등록된다() {
		Long postId = postService.create(me, TestPosts.meetup("금요일 저녁 번개", 4, List.of(kim))).id();
		Long roomId = meetupRoom(postId);
		assertThat(meetupMemberRepository.countByMeetupPostId(postId)).isEqualTo(2);

		ChatRoomInviteResponse invited = chatRoomService.invite(me, roomId, invite(lee));

		assertThat(invited.invitedCount()).isEqualTo(1);
		assertThat(invited.skippedUserIds()).isEmpty();
		assertThat(meetupMemberRepository.countByMeetupPostId(postId)).isEqualTo(3);
		assertThat(chatRoomService.getDetail(lee, roomId).members()).extracting(ChatUserResponse::id).contains(me, kim);
		assertThat(lastMessage(me, roomId)).isEqualTo("이사자님이 들어왔어요.");
	}

	@Test
	void 번개방은_정원을_넘겨_초대할_수_없다() {
		Long postId = postService.create(me, TestPosts.meetup("둘만 가는 번개", 2, List.of(kim))).id();
		Long roomId = meetupRoom(postId);

		assertError(() -> chatRoomService.invite(me, roomId, invite(lee)), ChatErrorCode.ROOM_FULL);
		assertThat(meetupMemberRepository.countByMeetupPostId(postId)).isEqualTo(2);
	}

	@Test
	void 번개방은_주최자가_아니면_초대할_수_없다() {
		Long postId = postService.create(me, TestPosts.meetup("금요일 저녁 번개", 5, List.of(kim))).id();
		Long roomId = meetupRoom(postId);

		assertError(() -> chatRoomService.invite(kim, roomId, invite(lee)), ChatErrorCode.FORBIDDEN_ACTION);
	}

	@Test
	void 함께_신청한_커피챗_방은_선배까지_여섯_명이_한도다() {
		Long senior = alumni("선배사자");
		Long roomId = coffeeChatRoom(senior, List.of(kim, lee));
		assertThat(chatRoomService.getDetail(me, roomId).members()).hasSize(3);

		Long park = alumni("박사자");
		Long choi = alumni("최사자");
		assertThat(chatRoomService.invite(me, roomId, new ChatRoomInviteRequest(List.of(park, choi))).invitedCount()).isEqualTo(2);
		assertThat(chatRoomService.getDetail(me, roomId).members()).hasSize(5);
		assertThat(lastMessage(me, roomId)).isEqualTo("박사자, 최사자님이 들어왔어요.");

		Long jung = alumni("정사자");
		assertError(() -> chatRoomService.invite(me, roomId, invite(jung)), ChatErrorCode.ROOM_FULL);
	}

	@Test
	void 모집에서_온_커피챗_방은_모집_정원을_따라간다() {
		Long senior = alumni("선배사자");
		Long roomId = recruitCoffeeChatRoom(senior, 3, List.of(kim));
		assertThat(chatRoomService.getDetail(me, roomId).members()).hasSize(2);

		Long park = alumni("박사자");
		assertThat(chatRoomService.invite(me, roomId, invite(park)).invitedCount()).isEqualTo(1);

		Long choi = alumni("최사자");
		assertError(() -> chatRoomService.invite(me, roomId, invite(choi)), ChatErrorCode.ROOM_FULL);
	}

	@Test
	void 커피챗_방은_신청자가_아니면_초대할_수_없다() {
		Long senior = alumni("선배사자");
		Long roomId = coffeeChatRoom(senior, List.of(kim));
		Long park = alumni("박사자");

		assertError(() -> chatRoomService.invite(senior, roomId, invite(park)), ChatErrorCode.FORBIDDEN_ACTION);
		assertError(() -> chatRoomService.invite(kim, roomId, invite(park)), ChatErrorCode.FORBIDDEN_ACTION);
	}

	@Test
	void 일대일_방에는_초대할_수_없다() {
		Long roomId = chatRequestService.create(me, new ChatRequestCreateRequest(kim, "안녕하세요! 이야기 나눠요.")).roomId();
		chatRequestService.accept(kim, roomId);

		assertError(() -> chatRoomService.invite(me, roomId, invite(lee)), ChatErrorCode.FORBIDDEN_ACTION);
	}

	@Test
	void 이미_참여_중이거나_승인되지_않은_회원은_건너뛴다() {
		Long postId = postService.create(me, TestPosts.meetup("금요일 저녁 번개", 10, List.of(kim))).id();
		Long roomId = meetupRoom(postId);
		Long pending = TestUsers.create(entityManager, "대기사자", UserStatus.PENDING);
		entityManager.flush();

		ChatRoomInviteResponse invited = chatRoomService.invite(me, roomId,
				new ChatRoomInviteRequest(List.of(kim, pending, lee)));

		assertThat(invited.invitedCount()).isEqualTo(1);
		assertThat(invited.skippedUserIds()).containsExactly(kim, pending);
		assertThat(meetupMemberRepository.countByMeetupPostId(postId)).isEqualTo(3);
	}

	@Test
	void 나갔던_사람을_초대하면_다시_들어온다() {
		Long senior = alumni("선배사자");
		Long roomId = coffeeChatRoom(senior, List.of(kim, lee));
		chatRoomService.leave(lee, roomId);
		assertError(() -> chatRoomService.getDetail(lee, roomId), ChatErrorCode.ROOM_NOT_FOUND);

		assertThat(chatRoomService.invite(me, roomId, invite(lee)).invitedCount()).isEqualTo(1);

		assertThat(chatRoomService.getDetail(lee, roomId).canSend()).isTrue();
		assertThat(lastMessage(me, roomId)).isEqualTo("이사자님이 들어왔어요.");
	}

	@Test
	void 취소된_번개방에는_초대할_수_없다() {
		Long postId = postService.create(me, TestPosts.meetup("금요일 저녁 번개", 5, List.of(kim))).id();
		Long roomId = meetupRoom(postId);
		postService.delete(me, postId);
		flush();

		assertError(() -> chatRoomService.invite(me, roomId, invite(lee)), ChatErrorCode.INVALID_ROOM_STATUS);
	}

	private ChatRoomInviteRequest invite(Long userId) {
		return new ChatRoomInviteRequest(List.of(userId));
	}

	private Long meetupRoom(Long postId) {
		flush();
		return chatRoomRepository.findByMeetupId(postId).map(ChatRoom::getId).orElseThrow();
	}

	private Long coffeeChatRoom(Long receiverId, List<Long> memberIds) {
		openGroupCoffeeChat(receiverId);
		Long chatId = coffeeChatService.create(me, new CoffeeChatCreateRequest(receiverId, "CAREER", null,
				"선배님 이야기를 듣고 싶어요.", null, PreferredMode.ONLINE, null, memberIds, null)).id();
		return accept(receiverId, chatId);
	}

	private Long recruitCoffeeChatRoom(Long receiverId, int capacity, List<Long> joiners) {
		openGroupCoffeeChat(receiverId);
		Long postId = postService.create(me, TestPosts.recruit("선배사자님께 같이 커피챗", receiverId, capacity)).id();
		joiners.forEach(joiner -> recruitService.join(joiner, postId));
		Long chatId = coffeeChatService.create(me, new CoffeeChatCreateRequest(null, "CAREER", null,
				"다 같이 이야기 나누고 싶어요.", null, PreferredMode.ONLINE, null, null, postId)).id();
		return accept(receiverId, chatId);
	}

	private void openGroupCoffeeChat(Long receiverId) {
		entityManager.persist(CoffeeSetting.builder().userId(receiverId).topics(List.of("CAREER"))
				.modes(List.of(MeetingMode.ONLINE)).build());
		entityManager.flush();
	}

	private Long accept(Long receiverId, Long chatId) {
		return coffeeChatService.accept(receiverId, chatId,
				new CoffeeChatAcceptRequest(List.of(LocalDateTime.now().plusDays(1)), null)).roomId();
	}

	private String lastMessage(Long userId, Long roomId) {
		flush();
		List<ChatMessageResponse> items = chatRoomService.getMessages(userId, roomId, null, null, 1).items();
		return items.get(items.size() - 1).content();
	}

	private Long alumni(String name) {
		Long userId = TestUsers.approved(entityManager, name);
		TestProfiles.save(entityManager, TestProfiles.profile(userId));
		return userId;
	}

	private void flush() {
		entityManager.flush();
		entityManager.clear();
	}

	private void assertError(ThrowingCallable callable, ErrorCode errorCode) {
		assertThatThrownBy(callable)
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(errorCode);
	}
}
