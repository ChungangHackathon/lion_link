package com.lionlink.backend.domain.profile.exception;

import com.lionlink.backend.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProfileErrorCode implements ErrorCode {

	DUPLICATE_ITEM(HttpStatus.CONFLICT, "이미 등록된 항목이에요."),
	ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 기본 정보를 입력했어요.");

	private final HttpStatus status;
	private final String message;
}
