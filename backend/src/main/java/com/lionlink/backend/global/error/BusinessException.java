package com.lionlink.backend.global.error;

import java.util.List;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;
	/** 400 검증 실패를 서비스에서 던질 때 필드별 메시지. 없으면 빈 목록. */
	private final List<FieldErrorResponse> fieldErrors;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
		this.fieldErrors = List.of();
	}

	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
		this.fieldErrors = List.of();
	}

	public BusinessException(ErrorCode errorCode, List<FieldErrorResponse> fieldErrors) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
		this.fieldErrors = fieldErrors;
	}
}
