package com.lionlink.backend.domain.admin.dto.response;

import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "가입 반려 결과")
public record RejectResponse(Long userId, UserStatus status, String rejectReason) {

	public static RejectResponse of(User user) {
		return new RejectResponse(user.getId(), user.getStatus(), user.getRejectReason());
	}
}
