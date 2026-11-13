package com.lionlink.backend.domain.coffeechat.event;

import java.util.List;

public record CoffeeChatNotificationEvent(
		List<Long> userIds,
		CoffeeChatNotificationType type,
		Long coffeeChatId,
		Long actorId,
		String message
) {
}
