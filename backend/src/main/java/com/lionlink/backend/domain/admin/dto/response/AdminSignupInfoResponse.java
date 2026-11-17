package com.lionlink.backend.domain.admin.dto.response;

import com.lionlink.backend.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이름·기수 수정 결과")
public record AdminSignupInfoResponse(Long userId, String name, Integer generation) {

	public static AdminSignupInfoResponse of(User user) {
		return new AdminSignupInfoResponse(user.getId(), user.getName(), user.getGeneration());
	}
}
