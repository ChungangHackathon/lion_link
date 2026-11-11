package com.lionlink.backend.domain.alumni.exception;

import com.lionlink.backend.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AlumniErrorCode implements ErrorCode {

	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "동문 정보를 찾을 수 없어요."),
	CANNOT_BOOKMARK_SELF(HttpStatus.BAD_REQUEST, "자기 자신은 관심 동문으로 저장할 수 없어요.");

	private final HttpStatus status;
	private final String message;
}
