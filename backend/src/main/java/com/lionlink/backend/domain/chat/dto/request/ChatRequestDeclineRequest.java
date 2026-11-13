package com.lionlink.backend.domain.chat.dto.request;

import com.lionlink.backend.domain.chat.entity.ChatDeclineReason;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatRequestDeclineRequest(
		@NotNull(message = "거절 사유를 골라 주세요.")
		ChatDeclineReason reason,

		@Size(max = 100, message = "사유는 100자 이하여야 합니다.")
		String message
) {

	@AssertTrue(message = "기타 사유는 5~100자로 입력해 주세요.")
	public boolean isMessageGivenForEtc() {
		return reason != ChatDeclineReason.ETC || (message != null && message.trim().length() >= 5);
	}
}
