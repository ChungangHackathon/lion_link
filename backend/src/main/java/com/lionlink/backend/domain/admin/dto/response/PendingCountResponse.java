package com.lionlink.backend.domain.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "승인 대기 건수")
public record PendingCountResponse(
		@Schema(description = "승인 대기 중인 회원 수 (탈퇴한 회원 제외)", example = "3") long pending
) {
}
