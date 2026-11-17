package com.lionlink.backend.domain.coffeechat.event;

import com.lionlink.backend.domain.notification.entity.Notification;
import com.lionlink.backend.domain.notification.entity.NotificationTab;
import com.lionlink.backend.domain.notification.entity.NotificationTargetType;
import com.lionlink.backend.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CoffeeChatNotificationListener {

	private final NotificationRepository notificationRepository;

	@EventListener
	public void onNotify(CoffeeChatNotificationEvent event) {
		notificationRepository.saveAll(event.userIds().stream()
				.map(userId -> Notification.builder()
						.userId(userId)
						.tab(NotificationTab.COFFEE_CHAT)
						.type(event.type().name())
						.targetType(NotificationTargetType.COFFEE_CHAT)
						.targetId(event.coffeeChatId())
						.actorId(event.actorId())
						.message(event.message())
						.build())
				.toList());
	}
}
