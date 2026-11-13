package com.lionlink.backend.domain.notification.dto.response;

import com.lionlink.backend.domain.notification.entity.Notification;
import com.lionlink.backend.domain.notification.entity.NotificationTab;
import com.lionlink.backend.domain.notification.entity.NotificationTargetType;
import java.time.LocalDateTime;

public record NotificationResponse(
		Long id,
		NotificationTab tab,
		String type,
		NotificationTargetType targetType,
		Long targetId,
		NotificationActorResponse actor,
		String message,
		boolean read,
		LocalDateTime createdAt
) {

	public static NotificationResponse of(Notification notification, NotificationActorResponse actor) {
		return new NotificationResponse(
				notification.getId(),
				notification.getTab(),
				notification.getType(),
				notification.getTargetType(),
				notification.getTargetId(),
				actor,
				notification.getMessage(),
				notification.isRead(),
				notification.getCreatedAt()
		);
	}
}
