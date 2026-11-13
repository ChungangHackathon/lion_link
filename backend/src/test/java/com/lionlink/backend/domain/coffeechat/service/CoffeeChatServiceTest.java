package com.lionlink.backend.domain.coffeechat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

import com.lionlink.backend.domain.alumni.dto.response.AlumniDetailResponse.CoffeeChatAvailability;
import com.lionlink.backend.domain.alumni.exception.AlumniErrorCode;
import com.lionlink.backend.domain.alumni.service.AlumniDetailService;
import com.lionlink.backend.domain.chat.entity.ChatMessage;
import com.lionlink.backend.domain.chat.entity.ChatMessageType;
import com.lionlink.backend.domain.chat.entity.ChatRoom;
import com.lionlink.backend.domain.chat.entity.ChatRoomStatus;
import com.lionlink.backend.domain.chat.entity.ChatRoomType;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatAcceptRequest;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatBox;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatCancelRequest;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatCreateRequest;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatFilter;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatRejectRequest;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatScheduleRequest;
import com.lionlink.backend.domain.coffeechat.dto.response.CoffeeChatAction;
import com.lionlink.backend.domain.coffeechat.dto.response.CoffeeChatCreateResponse;
import com.lionlink.backend.domain.coffeechat.dto.response.CoffeeChatDetailResponse;
import com.lionlink.backend.domain.coffeechat.dto.response.CoffeeChatListResponse;
import com.lionlink.backend.domain.coffeechat.dto.response.CoffeeChatRole;
import com.lionlink.backend.domain.coffeechat.dto.response.CoffeeChatSummaryResponse;
import com.lionlink.backend.domain.coffeechat.dto.response.CoffeeChatUserResponse;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChat;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatRejectReason;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatStatus;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeSetting;
import com.lionlink.backend.domain.coffeechat.entity.MeetingMode;
import com.lionlink.backend.domain.coffeechat.entity.PreferredMode;
import com.lionlink.backend.domain.coffeechat.exception.CoffeeChatErrorCode;
import com.lionlink.backend.domain.community.exception.CommunityErrorCode;
import com.lionlink.backend.domain.community.post.service.PostService;
import com.lionlink.backend.domain.community.recruit.entity.CoffeeChatRecruit;
import com.lionlink.backend.domain.community.recruit.entity.RecruitStatus;
import com.lionlink.backend.domain.community.recruit.service.RecruitService;
import com.lionlink.backend.domain.notification.entity.Notification;
import com.lionlink.backend.domain.notification.entity.NotificationTab;
import com.lionlink.backend.domain.notification.repository.NotificationRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.ErrorCode;
import com.lionlink.backend.support.TestPosts;
import com.lionlink.backend.support.TestProfiles;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CoffeeChatServiceTest {

	@Autowired
	private CoffeeChatService coffeeChatService;

	@Autowired
	private CoffeeChatQueryService coffeeChatQueryService;

	@Autowired
	private AlumniDetailService alumniDetailService;

	@Autowired
	private PostService postService;

	@Autowired
	private RecruitService recruitService;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private EntityManager entityManager;

	private Long me;
	private Long senior;
	private Long kim;
	private Long lee;

	@BeforeEach
	void setUp() {
		me = alumni("나사자");
		senior = alumni("선배사자");
		kim = alumni("김사자");
		lee = alumni("이사자");
		receiving(senior, true);
	}

	@Test
	void 신청하면_대기_중이고_받는_사자에게_알림이_간다() {
		Long id = create(me, request(senior, List.of())).id();

		CoffeeChat chat = entityManager.find(CoffeeChat.class, id);
		assertThat(chat.getStatus()).isEqualTo(CoffeeChatStatus.PENDING);
		assertThat(chat.getPreferredMode()).isEqualTo(PreferredMode.ANY);
		assertThat(chat.getExpiresAt()).isCloseTo(LocalDateTime.now().plusDays(14), within(5, ChronoUnit.SECONDS));
		List<Notification> notifications = notifications("COFFEE_CHAT_REQUESTED");
		assertThat(notifications).extracting(Notification::getUserId).containsExactly(senior);
		assertThat(notifications.get(0).getTab()).isEqualTo(NotificationTab.COFFEE_CHAT);
		assertThat(notifications.get(0).getTargetId()).isEqualTo(id);
		assertThat(notifications.get(0).getActorId()).isEqualTo(me);
		assertThat(notifications.get(0).getMessage()).isEqualTo("님이 커피챗을 신청했어요.");
	}

	@Test
	void 여러_명_신청은_함께_신청자를_저장한다() {
		Long id = create(me, request(senior, List.of(kim, lee))).id();

		CoffeeChatDetailResponse forKim = coffeeChatQueryService.getDetail(kim, id);
		assertThat(forKim.myRole()).isEqualTo(CoffeeChatRole.MEMBER);
		assertThat(forKim.members()).extracting(CoffeeChatUserResponse::id).containsExactlyInAnyOrder(kim, lee);
		assertThat(forKim.actions()).containsExactly(CoffeeChatAction.LEAVE);
		assertThat(notifications("COFFEE_CHAT_REQUESTED").get(0).getMessage()).isEqualTo("님 외 2명이 커피챗을 신청했어요.");
	}

	@Test
	void 신청_조건을_검사한다() {
		Long noSetting = alumni("설정없는사자");
		Long oneOnOne = alumni("일대일사자");
		receiving(oneOnOne, false);

		assertError(() -> create(me, request(me, List.of())), CoffeeChatErrorCode.CANNOT_REQUEST_SELF);
		assertError(() -> create(me, request(999_999L, List.of())), AlumniErrorCode.USER_NOT_FOUND);
		assertError(() -> create(me, request(noSetting, List.of())), CoffeeChatErrorCode.NOT_ACCEPTING);
		assertError(() -> create(me, request(oneOnOne, List.of(kim))), CoffeeChatErrorCode.GROUP_COFFEE_CHAT_NOT_ALLOWED);
		assertError(() -> create(me, request(senior, List.of(kim, kim))), CoffeeChatErrorCode.INVALID_MEMBERS);
		assertError(() -> create(me, request(senior, List.of(senior))), CoffeeChatErrorCode.INVALID_MEMBERS);
		assertError(() -> create(me, request(senior, List.of(999_999L))), CoffeeChatErrorCode.INVALID_MEMBERS);
		assertError(() -> create(me, withTopic(senior, "STARTUP", null)), CoffeeChatErrorCode.INVALID_TOPIC);
		assertError(() -> create(me, withTopic(senior, "ETC", " ")), CoffeeChatErrorCode.INVALID_TOPIC);

		Long id = create(me, withTopic(senior, "ETC", "포트폴리오 리뷰")).id();
		assertThat(entityManager.find(CoffeeChat.class, id).getTopicCustom()).isEqualTo("포트폴리오 리뷰");
	}

	@Test
	void 그_사자와_진행_중인_커피챗이_있으면_신청할_수_없다() {
		receiving(lee, true);
		create(kim, request(senior, List.of()));
		create(me, request(lee, List.of()));

		assertError(() -> create(kim, request(senior, List.of())), CoffeeChatErrorCode.OPEN_COFFEE_CHAT_EXISTS);
		assertError(() -> create(me, request(senior, List.of(kim))), CoffeeChatErrorCode.OPEN_COFFEE_CHAT_EXISTS);
		receiving(me, true);
		assertError(() -> create(lee, request(me, List.of())), CoffeeChatErrorCode.OPEN_COFFEE_CHAT_EXISTS);
	}

	@Test
	void 거절된_뒤_7일_동안은_다시_신청할_수_없다() {
		Long id = create(me, request(senior, List.of())).id();
		coffeeChatService.reject(senior, id, new CoffeeChatRejectRequest(CoffeeChatRejectReason.NO_TIME, "다음에 봬요"));

		String expected = LocalDate.now().plusDays(7).getMonthValue() + "월 " + LocalDate.now().plusDays(7).getDayOfMonth() + "일";
		assertThatThrownBy(() -> create(me, request(senior, List.of())))
				.isInstanceOf(BusinessException.class)
				.hasMessage(expected + "부터 다시 신청할 수 있어요.")
				.extracting("errorCode")
				.isEqualTo(CoffeeChatErrorCode.COOLDOWN);
		assertThat(alumniDetailService.getDetail(me, senior).coffeeChat().availability()).isEqualTo(CoffeeChatAvailability.COOLDOWN);
		assertThat(alumniDetailService.getDetail(me, senior).coffeeChat().nextAvailableDate()).isEqualTo(LocalDate.now().plusDays(7));

		update("update coffee_chats set rejected_at = dateadd('DAY', -7, current_timestamp) where id = " + id);
		assertThat(create(me, request(senior, List.of())).status()).isEqualTo(CoffeeChatStatus.PENDING);
	}

	@Test
	void 수락하면_1대1_채팅방이_열리고_신청자에게_알림이_간다() {
		Long id = create(me, request(senior, List.of())).id();
		LocalDateTime slot = LocalDateTime.now().plusDays(3).withNano(0);

		CoffeeChatDetailResponse detail = coffeeChatService.accept(senior, id, new CoffeeChatAcceptRequest(List.of(slot), "반가워요!"));

		assertThat(detail.status()).isEqualTo(CoffeeChatStatus.ACCEPTED);
		assertThat(detail.proposedSlots()).containsExactly(slot);
		assertThat(detail.responseMessage()).isEqualTo("반가워요!");
		assertThat(detail.actions()).containsExactly(CoffeeChatAction.SCHEDULE, CoffeeChatAction.CANCEL);
		ChatRoom room = entityManager.find(ChatRoom.class, detail.roomId());
		assertThat(room.getType()).isEqualTo(ChatRoomType.DIRECT);
		assertThat(room.getStatus()).isEqualTo(ChatRoomStatus.ACTIVE);
		assertThat(roomMemberIds(room.getId())).containsExactlyInAnyOrder(me, senior);
		assertThat(systemMessages(room.getId())).containsExactly("커피챗이 수락됐어요. 채팅으로 일정을 정해보세요.");
		assertThat(notifications("COFFEE_CHAT_ACCEPTED")).extracting(Notification::getUserId).containsExactly(me);
	}

	@Test
	void 거절했던_메시지_방이_있으면_그_방을_다시_연다() {
		ChatRoom declined = ChatRoom.builder().type(ChatRoomType.DIRECT).userAId(senior).userBId(me)
				.status(ChatRoomStatus.DECLINED).requestedBy(me).build();
		entityManager.persist(declined);
		entityManager.flush();
		Long id = create(me, request(senior, List.of())).id();

		Long roomId = coffeeChatService.accept(senior, id, new CoffeeChatAcceptRequest(List.of(LocalDateTime.now().plusDays(1)), null)).roomId();

		assertThat(roomId).isEqualTo(declined.getId());
		assertThat(entityManager.find(ChatRoom.class, roomId).getStatus()).isEqualTo(ChatRoomStatus.ACTIVE);
	}

	@Test
	void 여러_명_커피챗을_수락하면_단체_채팅방이_열린다() {
		Long id = create(me, request(senior, List.of(kim, lee))).id();

		Long roomId = coffeeChatService.accept(senior, id, new CoffeeChatAcceptRequest(List.of(LocalDateTime.now().plusDays(1)), null)).roomId();

		assertThat(entityManager.find(ChatRoom.class, roomId).getType()).isEqualTo(ChatRoomType.GROUP);
		assertThat(roomMemberIds(roomId)).containsExactlyInAnyOrder(senior, me, kim, lee);
		assertThat(notifications("COFFEE_CHAT_ACCEPTED")).extracting(Notification::getUserId).containsExactlyInAnyOrder(me, kim, lee);
	}

	@Test
	void 역할과_상태에_맞지_않는_동작은_막는다() {
		Long stranger = alumni("모르는사자");
		Long id = create(me, request(senior, List.of())).id();
		CoffeeChatAcceptRequest accept = new CoffeeChatAcceptRequest(List.of(LocalDateTime.now().plusDays(1)), null);

		assertError(() -> coffeeChatQueryService.getDetail(stranger, id), CoffeeChatErrorCode.COFFEE_CHAT_NOT_FOUND);
		assertError(() -> coffeeChatService.accept(stranger, id, accept), CoffeeChatErrorCode.COFFEE_CHAT_NOT_FOUND);
		assertError(() -> coffeeChatService.accept(me, id, accept), CoffeeChatErrorCode.FORBIDDEN_ACTION);
		assertError(() -> coffeeChatService.withdraw(senior, id), CoffeeChatErrorCode.FORBIDDEN_ACTION);
		assertError(() -> coffeeChatService.complete(me, id), CoffeeChatErrorCode.INVALID_STATUS);

		coffeeChatService.accept(senior, id, accept);
		assertError(() -> coffeeChatService.accept(senior, id, accept), CoffeeChatErrorCode.INVALID_STATUS);
		assertError(() -> coffeeChatService.withdraw(me, id), CoffeeChatErrorCode.INVALID_STATUS);

		Long expiredId = create(kim, request(senior, List.of())).id();
		update("update coffee_chats set expires_at = dateadd('MINUTE', -1, current_timestamp) where id = " + expiredId);
		assertThat(coffeeChatQueryService.getDetail(senior, expiredId).actions()).isEmpty();
		assertError(() -> coffeeChatService.accept(senior, expiredId, accept), CoffeeChatErrorCode.INVALID_STATUS);
	}

	@Test
	void 신청자는_대기_중에_신청을_취소할_수_있다() {
		Long id = create(me, request(senior, List.of())).id();

		CoffeeChatDetailResponse detail = coffeeChatService.withdraw(me, id);

		assertThat(detail.status()).isEqualTo(CoffeeChatStatus.CANCELED);
		assertThat(detail.actions()).isEmpty();
		assertThat(create(me, request(senior, List.of())).status()).isEqualTo(CoffeeChatStatus.PENDING);
	}

	@Test
	void 일정을_정하고_만남이_지나면_완료한다() {
		Long id = acceptedChat(List.of());
		LocalDateTime meetAt = LocalDateTime.now().plusDays(2).withSecond(0).withNano(0);

		CoffeeChatDetailResponse scheduled = coffeeChatService.schedule(me, id,
				new CoffeeChatScheduleRequest(meetAt, MeetingMode.OFFLINE, " 강남역 카페 ", "https://ignored.com"));

		assertThat(scheduled.status()).isEqualTo(CoffeeChatStatus.SCHEDULED);
		assertThat(scheduled.meetingPlace()).isEqualTo("강남역 카페");
		assertThat(scheduled.meetingLink()).isNull();
		assertThat(notifications("COFFEE_CHAT_SCHEDULED")).extracting(Notification::getUserId).containsExactly(senior);
		assertThat(systemMessages(scheduled.roomId())).last().asString().startsWith("일정이 확정됐어요. ");
		assertError(() -> coffeeChatService.complete(senior, id), CoffeeChatErrorCode.MEETING_NOT_PASSED);

		coffeeChatService.schedule(senior, id, new CoffeeChatScheduleRequest(meetAt.plusDays(1), MeetingMode.ONLINE, null, "https://meet.com/a"));
		assertThat(notifications("COFFEE_CHAT_SCHEDULED")).extracting(Notification::getMessage).last().asString().startsWith("님이 커피챗 일정을 바꿨어요.");

		update("update coffee_chats set scheduled_at = dateadd('HOUR', -1, current_timestamp) where id = " + id);
		assertThat(coffeeChatQueryService.getDetail(me, id).actions()).contains(CoffeeChatAction.COMPLETE);
		CoffeeChatDetailResponse completed = coffeeChatService.complete(me, id);
		assertThat(completed.status()).isEqualTo(CoffeeChatStatus.COMPLETED);
		assertThat(completed.completedAt()).isNotNull();
	}

	@Test
	void 수락_후에는_사유와_함께_취소한다() {
		Long id = acceptedChat(List.of());

		CoffeeChatDetailResponse canceled = coffeeChatService.cancel(senior, id, new CoffeeChatCancelRequest(" 급한 일정이 생겼어요 "));

		assertThat(canceled.status()).isEqualTo(CoffeeChatStatus.CANCELED);
		assertThat(canceled.cancelReason()).isEqualTo("급한 일정이 생겼어요");
		assertThat(canceled.canceledBy()).isEqualTo(senior);
		assertThat(notifications("COFFEE_CHAT_CANCELED")).extracting(Notification::getUserId, Notification::getActorId, Notification::getMessage)
				.containsExactly(tuple(me, senior, "님이 커피챗을 취소했어요."));
		assertThat(entityManager.find(ChatRoom.class, canceled.roomId()).getStatus()).isEqualTo(ChatRoomStatus.ACTIVE);
	}

	@Test
	void 함께_신청자는_빠질_수_있고_단체_채팅방에서도_나간다() {
		Long id = acceptedChat(List.of(kim, lee));
		Long roomId = coffeeChatQueryService.getDetail(me, id).roomId();

		coffeeChatService.leave(kim, id);
		flush();

		assertError(() -> coffeeChatQueryService.getDetail(kim, id), CoffeeChatErrorCode.COFFEE_CHAT_NOT_FOUND);
		assertThat(coffeeChatQueryService.getDetail(me, id).members()).extracting(CoffeeChatUserResponse::id).containsExactly(lee);
		assertThat(roomMemberIds(roomId)).containsExactlyInAnyOrder(senior, me, lee);
		assertError(() -> coffeeChatService.leave(me, id), CoffeeChatErrorCode.FORBIDDEN_ACTION);
	}

	@Test
	void 모집글에서_신청하면_참여자와_함께_신청하고_모집은_신청_완료가_된다() {
		Long postId = postService.create(me, TestPosts.recruit("선배사자님께 같이 커피챗", senior, 4)).id();
		recruitService.join(kim, postId);
		Long onlyAuthor = postService.create(lee, TestPosts.recruit("혼자 모집", senior, 4)).id();
		CoffeeChatCreateRequest fromRecruit = new CoffeeChatCreateRequest(null, "CAREER", null, "다 같이 이야기 나누고 싶어요.",
				null, PreferredMode.ONLINE, null, null, postId);

		assertError(() -> create(kim, fromRecruit), CommunityErrorCode.NOT_OWNER);
		assertError(() -> create(lee, new CoffeeChatCreateRequest(null, "CAREER", null, "다 같이 이야기 나누고 싶어요.",
				null, PreferredMode.ONLINE, null, null, onlyAuthor)), CoffeeChatErrorCode.INVALID_MEMBERS);

		Long id = create(me, fromRecruit).id();
		CoffeeChatDetailResponse detail = coffeeChatQueryService.getDetail(me, id);
		assertThat(detail.receiver().id()).isEqualTo(senior);
		assertThat(detail.members()).extracting(CoffeeChatUserResponse::id).containsExactly(kim);
		assertThat(detail.recruitPostId()).isEqualTo(postId);
		flush();
		CoffeeChatRecruit recruit = entityManager.find(CoffeeChatRecruit.class, postId);
		assertThat(recruit.getStatus()).isEqualTo(RecruitStatus.APPLIED);
		assertThat(recruit.getCoffeeChatId()).isEqualTo(id);
		assertError(() -> create(me, fromRecruit), CommunityErrorCode.RECRUIT_CLOSED);
	}

	@Test
	void 받은_신청과_보낸_신청을_상태별로_나눠_본다() {
		receiving(me, true);
		Long sent = create(me, request(senior, List.of(kim))).id();
		Long received = create(lee, request(me, List.of())).id();
		coffeeChatService.accept(me, received, new CoffeeChatAcceptRequest(List.of(LocalDateTime.now().plusDays(1)), null));

		CoffeeChatListResponse receivedBox = coffeeChatQueryService.getList(me, CoffeeChatBox.RECEIVED, CoffeeChatFilter.ALL, PageRequest.of(0, 20));
		assertThat(receivedBox.items()).extracting(CoffeeChatSummaryResponse::id).containsExactly(received);
		assertThat(receivedBox.items().get(0).counterpart().id()).isEqualTo(lee);
		assertThat(receivedBox.items().get(0).myRole()).isEqualTo(CoffeeChatRole.RECEIVER);
		assertThat(receivedBox.pendingReceivedCount()).isZero();
		assertThat(receivedBox.activeCount()).isEqualTo(1);

		CoffeeChatListResponse sentBox = coffeeChatQueryService.getList(me, CoffeeChatBox.SENT, CoffeeChatFilter.PENDING, PageRequest.of(0, 20));
		assertThat(sentBox.items()).extracting(CoffeeChatSummaryResponse::id).containsExactly(sent);
		assertThat(sentBox.items().get(0).counterpart().id()).isEqualTo(senior);
		assertThat(sentBox.items().get(0).members()).extracting(CoffeeChatUserResponse::id).containsExactly(kim);
		assertThat(coffeeChatQueryService.getList(me, CoffeeChatBox.SENT, CoffeeChatFilter.ACTIVE, PageRequest.of(0, 20)).items()).isEmpty();

		CoffeeChatListResponse kimSent = coffeeChatQueryService.getList(kim, CoffeeChatBox.SENT, CoffeeChatFilter.ALL, PageRequest.of(0, 20));
		assertThat(kimSent.items()).extracting(CoffeeChatSummaryResponse::myRole).containsExactly(CoffeeChatRole.MEMBER);
		assertThat(coffeeChatQueryService.getList(senior, CoffeeChatBox.RECEIVED, CoffeeChatFilter.ALL, PageRequest.of(0, 20))
				.pendingReceivedCount()).isEqualTo(1);
	}

	@Test
	void 답이_없는_신청은_만료되고_오래된_커피챗은_자동으로_완료된다() {
		Long expiring = create(me, request(senior, List.of())).id();
		update("update coffee_chats set expires_at = dateadd('MINUTE', -1, current_timestamp) where id = " + expiring);
		Long acceptedLongAgo = acceptedChatOf(kim);
		update("update coffee_chats set accepted_at = dateadd('DAY', -31, current_timestamp) where id = " + acceptedLongAgo);
		Long metLongAgo = acceptedChatOf(lee);
		coffeeChatService.schedule(lee, metLongAgo, new CoffeeChatScheduleRequest(LocalDateTime.now().plusDays(1), MeetingMode.ONLINE, null, null));
		update("update coffee_chats set scheduled_at = dateadd('DAY', -8, current_timestamp) where id = " + metLongAgo);

		assertThat(coffeeChatService.expireOverdue(LocalDateTime.now())).isEqualTo(1);
		assertThat(coffeeChatService.completeOverdue(LocalDateTime.now())).isEqualTo(2);
		flush();

		assertThat(entityManager.find(CoffeeChat.class, expiring).getStatus()).isEqualTo(CoffeeChatStatus.EXPIRED);
		assertThat(entityManager.find(CoffeeChat.class, expiring).getOpenFlag()).isNull();
		assertThat(notifications("COFFEE_CHAT_EXPIRED")).extracting(Notification::getUserId).containsExactly(me);
		assertThat(entityManager.find(CoffeeChat.class, acceptedLongAgo).getStatus()).isEqualTo(CoffeeChatStatus.COMPLETED);
		assertThat(entityManager.find(CoffeeChat.class, metLongAgo).getStatus()).isEqualTo(CoffeeChatStatus.COMPLETED);
		assertThat(alumniDetailService.getDetail(me, senior).coffeeChat().availability()).isEqualTo(CoffeeChatAvailability.COOLDOWN);
	}

	private Long acceptedChat(List<Long> memberIds) {
		Long id = create(me, request(senior, memberIds)).id();
		coffeeChatService.accept(senior, id, new CoffeeChatAcceptRequest(List.of(LocalDateTime.now().plusDays(1)), null));
		return id;
	}

	private Long acceptedChatOf(Long requesterId) {
		Long id = create(requesterId, request(senior, List.of())).id();
		coffeeChatService.accept(senior, id, new CoffeeChatAcceptRequest(List.of(LocalDateTime.now().plusDays(1)), null));
		return id;
	}

	private CoffeeChatCreateResponse create(Long userId, CoffeeChatCreateRequest request) {
		return coffeeChatService.create(userId, request);
	}

	private CoffeeChatCreateRequest request(Long receiverId, List<Long> memberIds) {
		return new CoffeeChatCreateRequest(receiverId, "CAREER", null, "선배님의 백엔드 경험이 궁금해요.",
				List.of("어떻게 공부하셨나요?"), PreferredMode.ANY, "평일 저녁", memberIds, null);
	}

	private CoffeeChatCreateRequest withTopic(Long receiverId, String topic, String topicCustom) {
		return new CoffeeChatCreateRequest(receiverId, topic, topicCustom, "선배님의 백엔드 경험이 궁금해요.",
				null, PreferredMode.ONLINE, null, null, null);
	}

	private Long alumni(String name) {
		Long userId = TestUsers.approved(entityManager, name);
		TestProfiles.save(entityManager, TestProfiles.profile(userId));
		return userId;
	}

	private void receiving(Long userId, boolean acceptGroup) {
		entityManager.persist(CoffeeSetting.builder().userId(userId).topics(List.of("CAREER", "PROJECT_DEV"))
				.modes(List.of(MeetingMode.ONLINE)).build());
		entityManager.flush();
		if (!acceptGroup) {
			update("update coffee_settings set accept_group = false where user_id = " + userId);
		}
		entityManager.clear();
	}

	private List<Notification> notifications(String type) {
		flush();
		return notificationRepository.findAll().stream().filter(notification -> notification.getType().equals(type)).toList();
	}

	private List<Long> roomMemberIds(Long roomId) {
		flush();
		return entityManager.createQuery("select m.userId from ChatRoomMember m where m.room.id = :roomId and m.leftAt is null", Long.class)
				.setParameter("roomId", roomId)
				.getResultList();
	}

	private List<String> systemMessages(Long roomId) {
		flush();
		return entityManager.createQuery("select m from ChatMessage m where m.room.id = :roomId order by m.id", ChatMessage.class)
				.setParameter("roomId", roomId)
				.getResultStream()
				.filter(message -> message.getType() == ChatMessageType.SYSTEM)
				.map(ChatMessage::getContent)
				.toList();
	}

	private void update(String sql) {
		entityManager.flush();
		entityManager.createNativeQuery(sql).executeUpdate();
		entityManager.clear();
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
