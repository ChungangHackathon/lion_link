package com.lionlink.backend.domain.auth.exception;

import com.lionlink.backend.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

	GOOGLE_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "구글 로그인에 실패했어요. 다시 시도해 주세요."),
	GOOGLE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "구글 서버와 연결할 수 없어요. 잠시 후 다시 시도해 주세요."),
	ALREADY_REGISTERED(HttpStatus.CONFLICT, "이미 가입된 구글 계정이에요.");

	private final HttpStatus status;
	private final String message;
}
