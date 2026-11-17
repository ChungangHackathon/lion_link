package com.lionlink.backend.domain.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageSendRequest(
		@NotBlank(message = "메시지를 입력해 주세요.")
		@Size(max = 1000, message = "메시지는 1000자까지 보낼 수 있어요.")
		String content,

		@Size(max = 36, message = "clientMsgId는 36자 이하여야 합니다.")
		String clientMsgId
) {
}
