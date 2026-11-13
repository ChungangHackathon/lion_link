package com.lionlink.backend.domain.coffeechat.dto.response;

import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatStatus;
import java.time.LocalDateTime;
import java.util.List;

public record CoffeeChatSummaryResponse(
		Long id,
		CoffeeChatStatus status,
		CoffeeChatRole myRole,
		CoffeeChatUserResponse counterpart,
		List<CoffeeChatUserResponse> members,
		String topic,
		String topicCustom,
		String reason,
		LocalDateTime createdAt,
		LocalDateTime expiresAt,
		LocalDateTime scheduledAt
) {
}
