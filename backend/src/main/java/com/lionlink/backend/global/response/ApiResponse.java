package com.lionlink.backend.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String code, String message, T data) {

	private static final String SUCCESS_CODE = "SUCCESS";
	private static final String SUCCESS_MESSAGE = "요청에 성공했습니다.";

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(true, SUCCESS_CODE, SUCCESS_MESSAGE, data);
	}

	public static ApiResponse<Void> ok() {
		return new ApiResponse<>(true, SUCCESS_CODE, SUCCESS_MESSAGE, null);
	}

	public static ApiResponse<Void> fail(String code, String message) {
		return new ApiResponse<>(false, code, message, null);
	}

	public static <T> ApiResponse<T> fail(String code, String message, T data) {
		return new ApiResponse<>(false, code, message, data);
	}
}
