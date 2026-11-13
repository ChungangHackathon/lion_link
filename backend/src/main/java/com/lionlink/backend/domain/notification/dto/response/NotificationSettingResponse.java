package com.lionlink.backend.domain.notification.dto.response;

import com.lionlink.backend.domain.notification.entity.NotificationSetting;

public record NotificationSettingResponse(
		boolean emailCoffeeChat,
		boolean emailMessageRequest,
		boolean emailSignupApproved
) {

	public static NotificationSettingResponse from(NotificationSetting setting) {
		return new NotificationSettingResponse(
				setting.isEmailCoffeeChat(),
				setting.isEmailMessageRequest(),
				setting.isEmailSignupApproved()
		);
	}
}
