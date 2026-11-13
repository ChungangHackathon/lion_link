package com.lionlink.backend.domain.coffeechat.exception;

import com.lionlink.backend.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CoffeeChatErrorCode implements ErrorCode {

	COFFEE_CHAT_NOT_FOUND(HttpStatus.NOT_FOUND, "커피챗을 찾을 수 없어요."),
	CANNOT_REQUEST_SELF(HttpStatus.BAD_REQUEST, "나에게는 커피챗을 신청할 수 없어요."),
	INVALID_TOPIC(HttpStatus.BAD_REQUEST, "사자가 받는 주제 중에서 골라 주세요."),
	INVALID_MEMBERS(HttpStatus.BAD_REQUEST, "함께 신청할 동문을 다시 확인해 주세요."),
	NOT_ACCEPTING(HttpStatus.CONFLICT, "지금은 커피챗을 받지 않는 사자예요."),
	GROUP_COFFEE_CHAT_NOT_ALLOWED(HttpStatus.CONFLICT, "이 사자는 지금 여러 명 커피챗을 받지 않아요."),
	OPEN_COFFEE_CHAT_EXISTS(HttpStatus.CONFLICT, "이 사자와 이미 진행 중인 커피챗이 있어요."),
	COOLDOWN(HttpStatus.CONFLICT, "아직 다시 신청할 수 없어요."),
	FORBIDDEN_ACTION(HttpStatus.FORBIDDEN, "이 커피챗에서 할 수 없는 동작이에요."),
	INVALID_STATUS(HttpStatus.CONFLICT, "지금 상태에서는 할 수 없어요."),
	MEETING_NOT_PASSED(HttpStatus.CONFLICT, "만남 시각이 지난 뒤에 완료할 수 있어요.");

	private final HttpStatus status;
	private final String message;
}
