package com.lionlink.backend.domain.admin.dto.response;

import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "가입 승인 결과")
public record ApproveResponse(
		Long userId,
		@Schema(description = "'{이름}님을 승인했어요' 토스트용") String name,
		UserStatus status,
		LocalDateTime reviewedAt
) {

	public static ApproveResponse of(User user) {
		return new ApproveResponse(user.getId(), user.getName(), user.getStatus(), user.getReviewedAt());
	}
}
