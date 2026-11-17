package com.lionlink.backend.domain.file.exception;

import com.lionlink.backend.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FileErrorCode implements ErrorCode {

	INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식이에요."),
	FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "업로드할 수 있는 파일 크기를 넘었어요."),
	INVALID_FILE_URL(HttpStatus.BAD_REQUEST, "올린 파일을 다시 확인해 주세요.");

	private final HttpStatus status;
	private final String message;
}
