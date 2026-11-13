package com.lionlink.backend.domain.chat.exception;

import com.lionlink.backend.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode implements ErrorCode {

	ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없어요."),
	MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "메시지를 찾을 수 없어요."),
	CANNOT_CHAT_SELF(HttpStatus.BAD_REQUEST, "나에게는 메시지를 보낼 수 없어요."),
	REQUEST_COOLDOWN(HttpStatus.CONFLICT, "아직 다시 요청할 수 없어요."),
	FORBIDDEN_ACTION(HttpStatus.FORBIDDEN, "이 채팅방에서 할 수 없는 동작이에요."),
	INVALID_ROOM_STATUS(HttpStatus.CONFLICT, "지금 상태에서는 할 수 없어요.");

	private final HttpStatus status;
	private final String message;
}
