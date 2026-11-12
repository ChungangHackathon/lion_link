package com.lionlink.backend.domain.community.event;

import com.lionlink.backend.domain.notification.entity.Notification;
import com.lionlink.backend.domain.notification.entity.NotificationTab;
import com.lionlink.backend.domain.notification.entity.NotificationTargetType;
import com.lionlink.backend.domain.notification.repository.NotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MeetupNotificationListener {

	static final String INVITED = "MEETUP_INVITED";
	static final String CANCELED = "MEETUP_CANCELED";

	private final NotificationRepository notificationRepository;

	@EventListener
	public void onInvited(MeetupInvitedEvent event) {
		String message = event.hostName() + "님이 번개에 초대했어요. " + event.title();
		save(event.inviteeIds(), INVITED, event.postId(), message);
	}

	@EventListener
	public void onCanceled(MeetupCanceledEvent event) {
		String message = "참여한 번개가 취소됐어요. " + event.title();
		save(event.memberIds(), CANCELED, event.postId(), message);
	}

	private void save(List<Long> userIds, String type, Long postId, String message) {
		notificationRepository.saveAll(userIds.stream()
				.map(userId -> Notification.builder()
						.userId(userId)
						.tab(NotificationTab.COMMUNITY)
						.type(type)
						.targetType(NotificationTargetType.POST)
						.targetId(postId)
						.message(message)
						.build())
				.toList());
	}
}
