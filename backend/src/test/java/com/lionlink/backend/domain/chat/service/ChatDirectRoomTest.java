package com.lionlink.backend.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionlink.backend.domain.chat.dto.request.ChatRequestCreateRequest;
import com.lionlink.backend.domain.chat.dto.request.ChatRequestDeclineRequest;
import com.lionlink.backend.domain.chat.dto.response.ChatDirectRoomResponse;
import com.lionlink.backend.domain.chat.entity.ChatDeclineReason;
import com.lionlink.backend.domain.chat.entity.ChatRoomStatus;
import com.lionlink.backend.domain.chat.exception.ChatErrorCode;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.ErrorCode;
import com.lionlink.backend.support.TestProfiles;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ChatDirectRoomTest {

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
	void 대화_중인_방을_찾아준다() {
		Long roomId = activeRoom(me, kim);

		ChatDirectRoomResponse found = chatRoomService.getDirectRoom(me, kim);

		assertThat(found.roomId()).isEqualTo(roomId);
		assertThat(found.status()).isEqualTo(ChatRoomStatus.ACTIVE);
		assertThat(found.requestedByMe()).isTrue();
		assertThat(chatRoomService.getDirectRoom(kim, me).requestedByMe()).isFalse();
	}

	@Test
	void 대기_중인_요청도_찾아준다() {
		Long roomId = chatRequestService.create(me, new ChatRequestCreateRequest(kim, "안녕하세요! 이야기 나눠요.")).roomId();

		ChatDirectRoomResponse found = chatRoomService.getDirectRoom(me, kim);

		assertThat(found.roomId()).isEqualTo(roomId);
		assertThat(found.status()).isEqualTo(ChatRoomStatus.REQUESTED);
	}

	@Test
	void 방이_없으면_찾을_수_없다() {
		assertError(() -> chatRoomService.getDirectRoom(me, kim), ChatErrorCode.ROOM_NOT_FOUND);
	}

	@Test
	void 거절된_방은_찾을_수_없다() {
		Long roomId = chatRequestService.create(me, new ChatRequestCreateRequest(kim, "안녕하세요! 이야기 나눠요.")).roomId();
		chatRequestService.decline(kim, roomId, new ChatRequestDeclineRequest(ChatDeclineReason.BUSY, null));

		assertError(() -> chatRoomService.getDirectRoom(me, kim), ChatErrorCode.ROOM_NOT_FOUND);
	}

	@Test
	void 내가_숨긴_방은_찾을_수_없다() {
		Long roomId = activeRoom(me, kim);
		chatRoomService.leave(me, roomId);

		assertError(() -> chatRoomService.getDirectRoom(me, kim), ChatErrorCode.ROOM_NOT_FOUND);
		assertThat(chatRoomService.getDirectRoom(kim, me).roomId()).isEqualTo(roomId);
	}

	@Test
	void 나_자신은_찾을_수_없다() {
		assertError(() -> chatRoomService.getDirectRoom(me, me), ChatErrorCode.CANNOT_CHAT_SELF);
	}

	private Long activeRoom(Long from, Long to) {
		Long roomId = chatRequestService.create(from, new ChatRequestCreateRequest(to, "안녕하세요! 이야기 나눠요.")).roomId();
		chatRequestService.accept(to, roomId);
		return roomId;
	}

	private Long alumni(String name) {
		Long userId = TestUsers.approved(entityManager, name);
		TestProfiles.save(entityManager, TestProfiles.profile(userId));
		return userId;
	}

	private void assertError(ThrowingCallable callable, ErrorCode errorCode) {
		assertThatThrownBy(callable)
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(errorCode);
	}
}
