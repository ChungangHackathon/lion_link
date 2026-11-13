package com.lionlink.backend.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionlink.backend.domain.alumni.exception.AlumniErrorCode;
import com.lionlink.backend.domain.alumni.service.AlumniDetailService;
import com.lionlink.backend.domain.chat.dto.request.ChatRequestCreateRequest;
import com.lionlink.backend.domain.chat.dto.request.ChatRequestDeclineRequest;
import com.lionlink.backend.domain.chat.dto.request.ChatRequestMessageUpdateRequest;
import com.lionlink.backend.domain.chat.dto.response.ChatMessageResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatRequestCreateResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatRequestSummaryResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatRoomDetailResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.lionlink.backend.domain.chat.entity.ChatDeclineReason;
import com.lionlink.backend.domain.chat.entity.ChatMessage;
import com.lionlink.backend.domain.chat.entity.ChatMessageType;
import com.lionlink.backend.domain.chat.entity.ChatRoom;
import com.lionlink.backend.domain.chat.entity.ChatRoomStatus;
import com.lionlink.backend.domain.chat.entity.ChatRoomType;
import com.lionlink.backend.domain.chat.exception.ChatErrorCode;
import com.lionlink.backend.domain.notification.entity.Notification;
import com.lionlink.backend.domain.notification.entity.NotificationTab;
import com.lionlink.backend.domain.notification.entity.NotificationTargetType;
import com.lionlink.backend.domain.notification.repository.NotificationRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.ErrorCode;
import com.lionlink.backend.support.TestProfiles;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
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
class ChatRequestServiceTest {

	@Autowired
	private ChatRequestService chatRequestService;

	@Autowired
	private ChatRoomService chatRoomService;

	@Autowired
	private AlumniDetailService alumniDetailService;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private EntityManager entityManager;

	private Long me;
	private Long kim;

	@BeforeEach
	void setUp() {
		me = alumni("나사자");
		kim = alumni("김사자");
	}

	@Test
	void 요청을_보내면_대기_중인_1대1_방이_생기고_받는_사람에게_알림이_간다() {
		ChatRequestCreateResponse response = request(me, kim, "안녕하세요! 백엔드 이야기 나누고 싶어요.");

		assertThat(response.created()).isTrue();
		assertThat(response.status()).isEqualTo(ChatRoomStatus.REQUESTED);
		ChatRoom room = room(response.roomId());
		assertThat(room.getType()).isEqualTo(ChatRoomType.DIRECT);
		assertThat(room.getUserAId()).isEqualTo(Math.min(me, kim));
		assertThat(room.getRequestedBy()).isEqualTo(me);

		List<Notification> notifications = notifications("MESSAGE_REQUESTED");
		assertThat(notifications).hasSize(1);
		assertThat(notifications.get(0).getUserId()).isEqualTo(kim);
		assertThat(notifications.get(0).getActorId()).isEqualTo(me);
		assertThat(notifications.get(0).getTab()).isEqualTo(NotificationTab.CHAT);
		assertThat(notifications.get(0).getTargetType()).isEqualTo(NotificationTargetType.ROOM);
		assertThat(notifications.get(0).getMessage()).isEqualTo("님이 메시지를 보냈어요.");

		List<ChatRequestSummaryResponse> received = chatRequestService.getReceived(kim, PageRequest.of(0, 20)).items();
		assertThat(received).hasSize(1);
		assertThat(received.get(0).requester().id()).isEqualTo(me);
		assertThat(received.get(0).firstMessage().content()).isEqualTo("안녕하세요! 백엔드 이야기 나누고 싶어요.");
		assertThat(chatRequestService.getReceived(me, PageRequest.of(0, 20)).items()).isEmpty();

		List<ChatRoomSummaryResponse> myRooms = chatRoomService.getList(me, PageRequest.of(0, 20)).items();
		assertThat(myRooms).extracting(ChatRoomSummaryResponse::status).containsExactly(ChatRoomStatus.REQUESTED);
		assertThat(myRooms.get(0).requestedByMe()).isTrue();
		assertThat(myRooms.get(0).partner().id()).isEqualTo(kim);
		assertThat(chatRoomService.getList(kim, PageRequest.of(0, 20)).items()).isEmpty();
	}

	@Test
	void 이미_요청_중이거나_대화_중이면_그_방을_돌려준다() {
		Long roomId = request(me, kim, "첫 번째 요청이에요.").roomId();

		ChatRequestCreateResponse again = request(me, kim, "다시 보내볼게요.");
		ChatRequestCreateResponse reverse = request(kim, me, "저도 보내볼게요.");

		assertThat(again.created()).isFalse();
		assertThat(again.roomId()).isEqualTo(roomId);
		assertThat(reverse.roomId()).isEqualTo(roomId);
		assertThat(reverse.requestedByMe()).isFalse();
		assertThat(notifications("MESSAGE_REQUESTED")).hasSize(1);
	}

	@Test
	void 받는_사람이_수락하면_대화가_시작되고_보낸_사람에게_알림이_간다() {
		Long roomId = request(me, kim, "안녕하세요! 이야기 나눠요.").roomId();

		assertError(() -> chatRequestService.accept(me, roomId), ChatErrorCode.FORBIDDEN_ACTION);
		ChatRoomDetailResponse detail = chatRequestService.accept(kim, roomId);

		assertThat(detail.status()).isEqualTo(ChatRoomStatus.ACTIVE);
		assertThat(detail.canSend()).isTrue();
		assertThat(detail.members()).extracting(member -> member.id()).containsExactly(me);
		assertThat(messages(roomId)).extracting(ChatMessage::getType).containsExactly(ChatMessageType.TEXT, ChatMessageType.SYSTEM);
		assertThat(notifications("MESSAGE_REQUEST_ACCEPTED")).extracting(Notification::getUserId, Notification::getActorId)
				.containsExactly(org.assertj.core.groups.Tuple.tuple(me, kim));
		assertError(() -> chatRequestService.accept(kim, roomId), ChatErrorCode.INVALID_ROOM_STATUS);
		assertThat(chatRoomService.getList(kim, PageRequest.of(0, 20)).items()).hasSize(1);
	}

	@Test
	void 거절하면_요청함에서_사라지고_7일_뒤에_다시_요청할_수_있다() {
		Long roomId = request(me, kim, "안녕하세요! 이야기 나눠요.").roomId();

		chatRequestService.decline(kim, roomId, new ChatRequestDeclineRequest(ChatDeclineReason.PREFER_COFFEE_CHAT, "무시되는 메시지"));
		flush();

		ChatRoom room = room(roomId);
		assertThat(room.getStatus()).isEqualTo(ChatRoomStatus.DECLINED);
		assertThat(room.getDeclineReason()).isEqualTo(ChatDeclineReason.PREFER_COFFEE_CHAT);
		assertThat(room.getDeclineMessage()).isNull();
		assertThat(chatRequestService.getReceived(kim, PageRequest.of(0, 20)).items()).isEmpty();
		assertThat(chatRoomService.getList(me, PageRequest.of(0, 20)).items())
				.extracting(ChatRoomSummaryResponse::status).containsExactly(ChatRoomStatus.DECLINED);

		LocalDate until = LocalDate.now().plusDays(7);
		assertThatThrownBy(() -> request(me, kim, "한 번만 더 요청해요."))
				.isInstanceOf(BusinessException.class)
				.hasMessage(until.getMonthValue() + "월 " + until.getDayOfMonth() + "일부터 다시 요청할 수 있어요.")
				.extracting("errorCode").isEqualTo(ChatErrorCode.REQUEST_COOLDOWN);
		assertThat(alumniDetailService.getDetail(me, kim).chat().requestAvailableDate()).isEqualTo(until);

		update("update chat_rooms set declined_at = dateadd('DAY', -7, current_timestamp) where id = " + roomId);
		ChatRequestCreateResponse retried = request(me, kim, "시간이 지나서 다시 요청해요.");

		assertThat(retried.created()).isTrue();
		assertThat(retried.roomId()).isEqualTo(roomId);
		assertThat(chatRequestService.getReceived(kim, PageRequest.of(0, 20)).items().get(0).firstMessage().content())
				.isEqualTo("시간이 지나서 다시 요청해요.");
	}

	@Test
	void 보낸_사람은_수락_전에_첫_메시지를_고치거나_요청을_취소할_수_있다() {
		Long roomId = request(me, kim, "처음 보내는 메시지예요.").roomId();
		Long messageId = messages(roomId).get(0).getId();

		ChatMessageResponse edited = chatRequestService.updateMessage(me, roomId, messageId,
				new ChatRequestMessageUpdateRequest(" 고친 메시지예요. "));
		assertThat(edited.content()).isEqualTo("고친 메시지예요.");
		assertThat(edited.editedAt()).isNotNull();
		assertError(() -> chatRequestService.updateMessage(kim, roomId, messageId, new ChatRequestMessageUpdateRequest("받은 사람이 고침")),
				ChatErrorCode.FORBIDDEN_ACTION);

		chatRequestService.cancel(me, roomId, messageId);
		flush();
		assertThat(room(roomId).getStatus()).isEqualTo(ChatRoomStatus.CANCELED);
		assertThat(entityManager.find(ChatMessage.class, messageId).isDeleted()).isTrue();
		assertThat(chatRequestService.getReceived(kim, PageRequest.of(0, 20)).items()).isEmpty();
		assertError(() -> chatRequestService.accept(kim, roomId), ChatErrorCode.INVALID_ROOM_STATUS);

		chatRoomService.leave(me, roomId);
		assertThat(chatRoomService.getList(me, PageRequest.of(0, 20)).items()).isEmpty();
	}

	@Test
	void 나에게나_없는_회원에게는_요청할_수_없다() {
		assertError(() -> request(me, me, "나에게 보내요."), ChatErrorCode.CANNOT_CHAT_SELF);
		assertError(() -> request(me, 999_999L, "없는 회원에게요."), AlumniErrorCode.USER_NOT_FOUND);
	}

	@Test
	void 대기_중인_요청은_나가기로_숨길_수_없다() {
		Long roomId = request(me, kim, "안녕하세요! 이야기 나눠요.").roomId();

		assertError(() -> chatRoomService.leave(me, roomId), ChatErrorCode.INVALID_ROOM_STATUS);
	}

	private ChatRequestCreateResponse request(Long from, Long to, String content) {
		return chatRequestService.create(from, new ChatRequestCreateRequest(to, content));
	}

	private Long alumni(String name) {
		Long userId = TestUsers.approved(entityManager, name);
		TestProfiles.save(entityManager, TestProfiles.profile(userId));
		return userId;
	}

	private ChatRoom room(Long roomId) {
		flush();
		return entityManager.find(ChatRoom.class, roomId);
	}

	private List<ChatMessage> messages(Long roomId) {
		flush();
		return entityManager.createQuery("select m from ChatMessage m where m.room.id = :roomId order by m.id", ChatMessage.class)
				.setParameter("roomId", roomId)
				.getResultList();
	}

	private List<Notification> notifications(String type) {
		flush();
		return notificationRepository.findAll().stream().filter(notification -> notification.getType().equals(type)).toList();
	}

	private void update(String sql) {
		flush();
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
