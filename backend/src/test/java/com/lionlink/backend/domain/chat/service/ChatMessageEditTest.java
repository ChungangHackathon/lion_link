package com.lionlink.backend.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionlink.backend.domain.chat.dto.request.ChatMessageSendRequest;
import com.lionlink.backend.domain.chat.dto.request.ChatMessageUpdateRequest;
import com.lionlink.backend.domain.chat.dto.request.ChatRequestCreateRequest;
import com.lionlink.backend.domain.chat.dto.response.ChatMessageResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.lionlink.backend.domain.chat.entity.ChatMessageType;
import com.lionlink.backend.domain.chat.exception.ChatErrorCode;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.ErrorCode;
import com.lionlink.backend.support.TestProfiles;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
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
class ChatMessageEditTest {

	@Autowired
	private ChatRoomService chatRoomService;

	@Autowired
	private ChatRequestService chatRequestService;

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
	void 내가_보낸_메시지를_수정하면_내용과_editedAt이_바뀐다() {
		Long roomId = activeRoom(me, kim);
		ChatMessageResponse sent = send(me, roomId, "오늘 3시에 봬요");

		ChatMessageResponse edited = chatRoomService.updateMessage(me, roomId, sent.id(), update("오늘 4시에 봬요"));

		assertThat(edited.id()).isEqualTo(sent.id());
		assertThat(edited.content()).isEqualTo("오늘 4시에 봬요");
		assertThat(edited.editedAt()).isNotNull();
		assertThat(edited.deleted()).isFalse();
		assertThat(messages(kim, roomId)).extracting(ChatMessageResponse::content).contains("오늘 4시에 봬요");
	}

	@Test
	void 지우면_내용은_비고_자리에_남는다() {
		Long roomId = activeRoom(me, kim);
		ChatMessageResponse sent = send(me, roomId, "잘못 보냈어요");

		ChatMessageResponse removed = chatRoomService.deleteMessage(me, roomId, sent.id());

		assertThat(removed.deleted()).isTrue();
		assertThat(removed.content()).isNull();
		assertThat(messages(kim, roomId)).extracting(ChatMessageResponse::id).contains(sent.id());
		assertThat(messages(kim, roomId)).filteredOn(ChatMessageResponse::deleted).hasSize(1);
	}

	@Test
	void 지운_메시지는_목록_미리보기에도_지워진_것으로_보인다() {
		Long roomId = activeRoom(me, kim);
		ChatMessageResponse sent = send(me, roomId, "마지막 메시지");

		chatRoomService.deleteMessage(me, roomId, sent.id());
		flush();

		ChatRoomSummaryResponse forKim = chatRoomService.getList(kim, PageRequest.of(0, 20)).items().get(0);
		assertThat(forKim.lastMessage().deleted()).isTrue();
		assertThat(forKim.lastMessage().content()).isNull();
	}

	@Test
	void 남이_보낸_메시지는_고치거나_지울_수_없다() {
		Long roomId = activeRoom(me, kim);
		ChatMessageResponse sent = send(me, roomId, "내 메시지");

		assertError(() -> chatRoomService.updateMessage(kim, roomId, sent.id(), update("남의 말을 바꿔요")), ChatErrorCode.MESSAGE_NOT_FOUND);
		assertError(() -> chatRoomService.deleteMessage(kim, roomId, sent.id()), ChatErrorCode.MESSAGE_NOT_FOUND);
	}

	@Test
	void 이미_지운_메시지는_다시_건드릴_수_없다() {
		Long roomId = activeRoom(me, kim);
		ChatMessageResponse sent = send(me, roomId, "지울 메시지");
		chatRoomService.deleteMessage(me, roomId, sent.id());

		assertError(() -> chatRoomService.deleteMessage(me, roomId, sent.id()), ChatErrorCode.MESSAGE_NOT_FOUND);
		assertError(() -> chatRoomService.updateMessage(me, roomId, sent.id(), update("되살려요")), ChatErrorCode.MESSAGE_NOT_FOUND);
	}

	@Test
	void 안내_메시지는_고치거나_지울_수_없다() {
		Long roomId = activeRoom(me, kim);
		Long systemId = messages(me, roomId).stream()
				.filter(message -> message.type() == ChatMessageType.SYSTEM)
				.findFirst()
				.orElseThrow()
				.id();

		assertError(() -> chatRoomService.deleteMessage(me, roomId, systemId), ChatErrorCode.MESSAGE_NOT_FOUND);
	}

	@Test
	void 수락되지_않은_방에서는_할_수_없다() {
		Long roomId = chatRequestService.create(me, new ChatRequestCreateRequest(kim, "안녕하세요! 이야기 나눠요.")).roomId();
		Long firstId = messages(me, roomId).get(0).id();

		assertError(() -> chatRoomService.updateMessage(me, roomId, firstId, update("요청 메시지를 바꿔요")), ChatErrorCode.INVALID_ROOM_STATUS);
		assertError(() -> chatRoomService.deleteMessage(me, roomId, firstId), ChatErrorCode.INVALID_ROOM_STATUS);
	}

	@Test
	void 참여하지_않은_방에서는_할_수_없다() {
		Long roomId = activeRoom(me, kim);
		ChatMessageResponse sent = send(me, roomId, "내 메시지");
		Long lee = alumni("이사자");

		assertError(() -> chatRoomService.deleteMessage(lee, roomId, sent.id()), ChatErrorCode.ROOM_NOT_FOUND);
	}

	private ChatMessageUpdateRequest update(String content) {
		return new ChatMessageUpdateRequest(content);
	}

	private Long activeRoom(Long from, Long to) {
		Long roomId = chatRequestService.create(from, new ChatRequestCreateRequest(to, "안녕하세요! 이야기 나눠요.")).roomId();
		chatRequestService.accept(to, roomId);
		return roomId;
	}

	private ChatMessageResponse send(Long userId, Long roomId, String content) {
		return chatRoomService.send(userId, roomId, new ChatMessageSendRequest(content, null));
	}

	private List<ChatMessageResponse> messages(Long userId, Long roomId) {
		flush();
		return chatRoomService.getMessages(userId, roomId, null, null, 30).items();
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
