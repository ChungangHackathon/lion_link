package com.lionlink.backend.domain.coffeechat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CoffeeChatCancelRequest(
		@NotBlank(message = "취소 사유를 입력해 주세요.")
		@Size(min = 5, max = 200, message = "취소 사유는 5~200자여야 합니다.")
		String cancelReason
) {
}
