package com.lionlink.backend.global.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(String code, String message, List<FieldErrorResponse> fieldErrors) {

	public static ErrorResponse of(ErrorCode errorCode) {
		return of(errorCode, errorCode.getMessage());
	}

	public static ErrorResponse of(ErrorCode errorCode, String message) {
		return new ErrorResponse(errorCode.getCode(), message, List.of());
	}

	public static ErrorResponse of(ErrorCode errorCode, List<FieldErrorResponse> fieldErrors) {
		return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), fieldErrors);
	}
}
