package com.lionlink.backend.domain.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** 보낸 항목만 바꾼다. 빠진 항목은 그대로 둔다. */
public record NotificationSettingUpdateRequest(
		@Schema(description = "커피챗 신청 메일 받기", nullable = true) Boolean emailCoffeeChat,
		@Schema(description = "메시지 요청 메일 받기", nullable = true) Boolean emailMessageRequest,
		@Schema(description = "가입 승인 메일 받기", nullable = true) Boolean emailSignupApproved
) {
}
