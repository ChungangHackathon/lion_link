package com.lionlink.backend.domain.admin.exception;

import com.lionlink.backend.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AdminErrorCode implements ErrorCode {

	ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 가입이에요.");

	private final HttpStatus status;
	private final String message;
}
