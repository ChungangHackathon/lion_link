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
public class CommunityNotificationListener {

	static final String MEETUP_INVITED = "MEETUP_INVITED";
	static final String MEETUP_CANCELED = "MEETUP_CANCELED";
	static final String RECRUIT_JOINED = "RECRUIT_JOINED";

	private final NotificationRepository notificationRepository;

	@EventListener
	public void onMeetupInvited(MeetupInvitedEvent event) {
		String message = event.hostName() + "님이 번개에 초대했어요. " + event.title();
		save(event.inviteeIds(), MEETUP_INVITED, event.postId(), message);
	}

	@EventListener
	public void onMeetupCanceled(MeetupCanceledEvent event) {
		String message = "참여한 번개가 취소됐어요. " + event.title();
		save(event.memberIds(), MEETUP_CANCELED, event.postId(), message);
	}

	@EventListener
	public void onRecruitJoined(RecruitJoinedEvent event) {
		String message = event.memberName() + "님이 내 모집글에 참여했어요. " + event.title();
		save(List.of(event.authorId()), RECRUIT_JOINED, event.postId(), message);
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
