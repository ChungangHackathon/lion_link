package com.lionlink.backend.domain.coffeechat.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record CoffeeChatAcceptRequest(
		@NotEmpty(message = "가능한 시간을 1개 이상 알려주세요.")
		@Size(max = 3, message = "가능한 시간은 최대 3개까지 알려줄 수 있어요.")
		List<@NotNull @Future(message = "지금 이후 시간으로 골라 주세요.") LocalDateTime> proposedSlots,

		@Size(max = 200, message = "한마디는 200자 이하여야 합니다.")
		String responseMessage
) {
}
