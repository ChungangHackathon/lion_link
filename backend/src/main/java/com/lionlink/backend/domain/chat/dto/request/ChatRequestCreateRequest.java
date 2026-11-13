package com.lionlink.backend.domain.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatRequestCreateRequest(
		@NotNull(message = "메시지를 받을 동문을 골라 주세요.")
		Long receiverId,

		@NotBlank(message = "첫 메시지를 입력해 주세요.")
		@Size(min = 5, max = 300, message = "첫 메시지는 5~300자여야 합니다.")
		String content
) {
}
