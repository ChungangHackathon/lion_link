package com.lionlink.backend.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

	VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요."),
	UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "로그인이 필요해요."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없어요."),
	NOT_APPROVED(HttpStatus.FORBIDDEN, "가입 승인 후 이용할 수 있어요."),
	PROFILE_NOT_COMPLETED(HttpStatus.FORBIDDEN, "기본 프로필을 먼저 입력해 주세요."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 대상을 찾을 수 없어요."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식이에요."),
	CONFLICT(HttpStatus.CONFLICT, "이미 존재하거나 다른 데이터와 충돌해요."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했어요.");

	private final HttpStatus status;
	private final String message;
}
