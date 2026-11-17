package com.lionlink.backend.domain.notification.exception;

import com.lionlink.backend.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

	NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없어요."),
	INVALID_UNSUBSCRIBE_TOKEN(HttpStatus.BAD_REQUEST, "수신 거부 링크가 올바르지 않아요.");

	private final HttpStatus status;
	private final String message;
}
