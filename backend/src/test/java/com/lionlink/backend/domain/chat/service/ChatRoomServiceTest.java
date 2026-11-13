package com.lionlink.backend.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionlink.backend.domain.chat.dto.request.ChatMessageSendRequest;
import com.lionlink.backend.domain.chat.dto.request.ChatPreferenceUpdateRequest;
import com.lionlink.backend.domain.chat.dto.request.ChatReadRequest;
import com.lionlink.backend.domain.chat.dto.request.ChatRequestCreateRequest;
import com.lionlink.backend.domain.chat.dto.response.ChatMessageListResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatMessageResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatRoomDetailResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.lionlink.backend.domain.chat.exception.ChatErrorCode;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatAcceptRequest;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatCreateRequest;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatStatus;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeSetting;
import com.lionlink.backend.domain.coffeechat.entity.MeetingMode;
import com.lionlink.backend.domain.coffeechat.entity.PreferredMode;
import com.lionlink.backend.domain.coffeechat.service.CoffeeChatService;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.ErrorCode;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ChatRoomServiceTest {

	@Autowired
	private ChatRoomService chatRoomService;

	@Autowired
	private ChatRequestService chatRequestService;

	@Autowired
	private CoffeeChatService coffeeChatService;

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
	void 메시지를_보내면_상대_목록에_마지막_메시지와_안_읽은_수가_보인다() {
		Long roomId = activeRoom(me, kim);

		send(me, roomId, "안녕하세요");
		ChatMessageResponse last = send(me, roomId, "반가워요");

		assertThat(last.unreadCount()).isEqualTo(1);
		ChatRoomSummaryResponse forKim = chatRoomService.getList(kim, PageRequest.of(0, 20)).items().get(0);
		assertThat(forKim.lastMessage().content()).isEqualTo("반가워요");
		assertThat(forKim.unreadCount()).isEqualTo(2);
		assertThat(chatRoomService.getList(me, PageRequest.of(0, 20)).items().get(0).unreadCount()).isZero();

		assertThat(chatRoomService.read(kim, roomId, null).lastReadMessageId()).isEqualTo(last.id());
		assertThat(chatRoomService.getList(kim, PageRequest.of(0, 20)).items().get(0).unreadCount()).isZero();
		assertThat(lastMessage(kim, roomId).unreadCount()).isZero();
		assertThat(chatRoomService.read(kim, roomId, new ChatReadRequest(last.id() - 1)).lastReadMessageId()).isEqualTo(last.id());
	}

	@Test
	void 같은_clientMsgId로_다시_보내면_한_번만_저장된다() {
		Long roomId = activeRoom(me, kim);

		ChatMessageResponse first = chatRoomService.send(me, roomId, new ChatMessageSendRequest("한 번만 저장돼요", "uuid-1"));
		ChatMessageResponse retry = chatRoomService.send(me, roomId, new ChatMessageSendRequest("한 번만 저장돼요", "uuid-1"));

		assertThat(retry.id()).isEqualTo(first.id());
		assertThat(chatRoomService.getMessages(me, roomId, null, null, 30).items())
				.filteredOn(message -> "한 번만 저장돼요".equals(message.content())).hasSize(1);
	}

	@Test
	void 대화_중이_아니거나_내가_없는_방에는_보낼_수_없다() {
		Long requested = chatRequestService.create(me, new ChatRequestCreateRequest(kim, "안녕하세요! 이야기 나눠요.")).roomId();
		Long stranger = alumni("모르는사자");

		assertError(() -> send(me, requested, "수락 전에 보내요"), ChatErrorCode.INVALID_ROOM_STATUS);
		assertError(() -> send(stranger, requested, "남의 방이에요"), ChatErrorCode.ROOM_NOT_FOUND);
		assertError(() -> chatRoomService.getDetail(stranger, requested), ChatErrorCode.ROOM_NOT_FOUND);
	}

	@Test
	void 메시지는_오래된_것부터_정렬하고_before와_after로_이어_받는다() {
		Long roomId = activeRoom(me, kim);
		List<Long> ids = List.of("1", "2", "3", "4", "5").stream().map(text -> send(me, roomId, "메시지 " + text).id()).toList();

		ChatMessageListResponse latest = chatRoomService.getMessages(me, roomId, null, null, 2);
		assertThat(latest.items()).extracting(ChatMessageResponse::id).containsExactly(ids.get(3), ids.get(4));
		assertThat(latest.hasMore()).isTrue();

		ChatMessageListResponse older = chatRoomService.getMessages(me, roomId, ids.get(3), null, 2);
		assertThat(older.items()).extracting(ChatMessageResponse::id).containsExactly(ids.get(1), ids.get(2));

		ChatMessageListResponse newer = chatRoomService.getMessages(me, roomId, null, ids.get(2), 30);
		assertThat(newer.items()).extracting(ChatMessageResponse::id).containsExactly(ids.get(3), ids.get(4));
		assertThat(newer.hasMore()).isFalse();
	}

	@Test
	void 단체방은_메시지마다_안_읽은_사람_수를_센다() {
		Long senior = alumni("선배사자");
		Long lee = alumni("이사자");
		entityManager.persist(CoffeeSetting.builder().userId(senior).topics(List.of("CAREER")).modes(List.of(MeetingMode.ONLINE)).build());
		entityManager.flush();
		Long chatId = coffeeChatService.create(me, new CoffeeChatCreateRequest(senior, "CAREER", null, "선배님 이야기를 듣고 싶어요.",
				null, PreferredMode.ONLINE, null, List.of(kim, lee), null)).id();
		Long roomId = coffeeChatService.accept(senior, chatId, new CoffeeChatAcceptRequest(List.of(LocalDateTime.now().plusDays(1)), null)).roomId();

		ChatMessageResponse message = send(senior, roomId, "다들 반가워요");
		assertThat(message.unreadCount()).isEqualTo(3);
		chatRoomService.read(kim, roomId, null);
		assertThat(lastMessage(senior, roomId).unreadCount()).isEqualTo(2);

		ChatRoomDetailResponse detail = chatRoomService.getDetail(me, roomId);
		assertThat(detail.members()).hasSize(3);
		assertThat(detail.coffeeChat().id()).isEqualTo(chatId);
		assertThat(detail.coffeeChat().status()).isEqualTo(CoffeeChatStatus.ACCEPTED);

		chatRoomService.leave(lee, roomId);
		assertThat(lastMessage(me, roomId).content()).isEqualTo("이사자님이 나갔어요.");
		assertError(() -> chatRoomService.getDetail(lee, roomId), ChatErrorCode.ROOM_NOT_FOUND);
	}

	@Test
	void 일대일_방을_나가면_목록에서_사라지고_상대가_보내면_새_메시지만_보인다() {
		Long roomId = activeRoom(me, kim);
		send(kim, roomId, "예전 대화예요");

		chatRoomService.leave(me, roomId);
		assertThat(chatRoomService.getList(me, PageRequest.of(0, 20)).items()).isEmpty();

		send(kim, roomId, "다시 연락드려요");
		flush();
		List<ChatRoomSummaryResponse> rooms = chatRoomService.getList(me, PageRequest.of(0, 20)).items();
		assertThat(rooms).hasSize(1);
		assertThat(rooms.get(0).unreadCount()).isEqualTo(1);
		assertThat(chatRoomService.getMessages(me, roomId, null, null, 30).items())
				.extracting(ChatMessageResponse::content).containsExactly("다시 연락드려요");
	}

	@Test
	void 상단_고정한_방이_먼저_오고_알림을_끌_수_있다() {
		Long lee = alumni("이사자");
		Long first = activeRoom(me, kim);
		Long second = activeRoom(me, lee);
		send(lee, second, "최근 메시지");

		assertThat(chatRoomService.getList(me, PageRequest.of(0, 20)).items())
				.extracting(ChatRoomSummaryResponse::roomId).containsExactly(second, first);

		assertThat(chatRoomService.updatePreferences(me, first, new ChatPreferenceUpdateRequest(true, true)).pinned()).isTrue();
		List<ChatRoomSummaryResponse> rooms = chatRoomService.getList(me, PageRequest.of(0, 20)).items();
		assertThat(rooms).extracting(ChatRoomSummaryResponse::roomId).containsExactly(first, second);
		assertThat(rooms.get(0).muted()).isTrue();

		assertThat(chatRoomService.updatePreferences(me, first, new ChatPreferenceUpdateRequest(false, null)).muted()).isTrue();
	}

	private Long activeRoom(Long from, Long to) {
		Long roomId = chatRequestService.create(from, new ChatRequestCreateRequest(to, "안녕하세요! 이야기 나눠요.")).roomId();
		chatRequestService.accept(to, roomId);
		return roomId;
	}

	private ChatMessageResponse send(Long userId, Long roomId, String content) {
		return chatRoomService.send(userId, roomId, new ChatMessageSendRequest(content, null));
	}

	private ChatMessageResponse lastMessage(Long userId, Long roomId) {
		flush();
		List<ChatMessageResponse> items = chatRoomService.getMessages(userId, roomId, null, null, 1).items();
		return items.get(items.size() - 1);
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
