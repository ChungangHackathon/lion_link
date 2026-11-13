package com.lionlink.backend.domain.coffeechat.dto.request;

import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatRejectReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CoffeeChatRejectRequest(
		@NotNull(message = "거절 사유를 골라 주세요.")
		CoffeeChatRejectReason rejectReason,

		@Size(max = 200, message = "메시지는 200자 이하여야 합니다.")
		String responseMessage
) {
}
