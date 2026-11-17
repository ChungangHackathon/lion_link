package com.lionlink.backend.global.error;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

	HttpStatus getStatus();

	String getMessage();

	String name();

	default String getCode() {
		return name();
	}
}
