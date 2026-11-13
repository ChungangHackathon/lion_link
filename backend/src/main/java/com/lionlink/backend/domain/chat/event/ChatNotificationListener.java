package com.lionlink.backend.domain.chat.event;

import com.lionlink.backend.domain.notification.entity.Notification;
import com.lionlink.backend.domain.notification.entity.NotificationTab;
import com.lionlink.backend.domain.notification.entity.NotificationTargetType;
import com.lionlink.backend.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatNotificationListener {

	private final NotificationRepository notificationRepository;

	@EventListener
	public void onNotify(ChatNotificationEvent event) {
		notificationRepository.save(Notification.builder()
				.userId(event.userId())
				.tab(NotificationTab.CHAT)
				.type(event.type())
				.targetType(NotificationTargetType.ROOM)
				.targetId(event.roomId())
				.actorId(event.actorId())
				.message(event.message())
				.build());
	}
}
