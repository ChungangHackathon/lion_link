package com.lionlink.backend.domain.admin.dto.response;

import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "선택 승인 결과. 일부가 실패해도 나머지는 승인된다")
public record ApproveBatchResponse(List<Succeeded> succeeded, List<Failed> failed) {

	public record Succeeded(Long userId, String name, UserStatus status) {

		public static Succeeded of(User user) {
			return new Succeeded(user.getId(), user.getName(), user.getStatus());
		}
	}

	public record Failed(
			Long userId,
			@Schema(description = "ALREADY_PROCESSED 또는 NOT_FOUND") String code
	) {
	}
}
