package com.lionlink.backend.domain.chat.dto.response;

import java.time.LocalDateTime;

public record ChatRequestSummaryResponse(
		Long roomId,
		ChatUserResponse requester,
		ChatMessageResponse firstMessage,
		LocalDateTime requestedAt
) {
}
