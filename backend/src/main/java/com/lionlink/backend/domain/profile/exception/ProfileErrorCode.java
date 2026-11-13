package com.lionlink.backend.domain.profile.exception;

import com.lionlink.backend.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProfileErrorCode implements ErrorCode {

	DUPLICATE_ITEM(HttpStatus.CONFLICT, "이미 등록된 항목이에요."),
	ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 기본 정보를 입력했어요."),
	LIMIT_EXCEEDED(HttpStatus.CONFLICT, "더 이상 추가할 수 없어요. 최대 개수를 넘었어요."),
	DEFAULT_ITEM_NOT_DELETABLE(HttpStatus.FORBIDDEN, "가입 기수 항목은 삭제할 수 없어요.");

	private final HttpStatus status;
	private final String message;
}
