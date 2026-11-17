package com.lionlink.backend.domain.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "가입 반려 요청")
public record RejectRequest(
		@Schema(description = "UNVERIFIED 동문인지 확인할 수 없어요, MISMATCH 이름·기수가 맞지 않아요, ETC 기타")
		@NotNull(message = "반려 사유를 선택해 주세요.") RejectReasonType reasonType,
		@Schema(description = "ETC일 때 필수(5~100자). ETC가 아니면 무시한다") String reasonText
) {
}
