package com.lionlink.backend.domain.notification.dto.response;

import java.util.List;

public record NotificationListResponse(
		List<NotificationResponse> items,
		UnreadCountResponse unreadCounts
) {
}
