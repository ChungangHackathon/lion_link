package com.lionlink.backend.domain.community.event;

import com.lionlink.backend.domain.notification.entity.Notification;
import com.lionlink.backend.domain.notification.entity.NotificationTab;
import com.lionlink.backend.domain.notification.entity.NotificationTargetType;
import com.lionlink.backend.domain.notification.repository.NotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 커뮤니티 알림을 저장한다. 회원이 일으킨 알림은 actorId를 남기고, message에는 이름 뒤에 붙는 문구만 쓴다.
 * 화면은 지금 이름을 굵게 쓰고 message를 이어 붙인다(알림 드롭다운 H-07).
 */
@Component
@RequiredArgsConstructor
public class CommunityNotificationListener {

	static final String MEETUP_INVITED = "MEETUP_INVITED";
	static final String MEETUP_CANCELED = "MEETUP_CANCELED";
	static final String RECRUIT_JOINED = "RECRUIT_JOINED";
	static final String RECRUIT_CANCELED = "RECRUIT_CANCELED";
	static final String RECRUIT_CLOSED = "RECRUIT_CLOSED";
	static final String COMMENT_CREATED = "COMMENT_CREATED";

	private final NotificationRepository notificationRepository;

	@EventListener
	public void onMeetupInvited(MeetupInvitedEvent event) {
		String message = "님이 번개에 초대했어요. " + event.title();
		save(event.inviteeIds(), MEETUP_INVITED, event.postId(), event.hostId(), message);
	}

	@EventListener
	public void onMeetupCanceled(MeetupCanceledEvent event) {
		String message = "참여한 번개가 취소됐어요. " + event.title();
		save(event.memberIds(), MEETUP_CANCELED, event.postId(), null, message);
	}

	@EventListener
	public void onRecruitJoined(RecruitJoinedEvent event) {
		String message = "님이 내 모집글에 참여했어요. " + event.title();
		save(List.of(event.authorId()), RECRUIT_JOINED, event.postId(), event.memberId(), message);
	}

	@EventListener
	public void onRecruitCanceled(RecruitCanceledEvent event) {
		String message = "참여한 커피챗 모집이 취소됐어요. " + event.title();
		save(event.memberIds(), RECRUIT_CANCELED, event.postId(), null, message);
	}

	@EventListener
	public void onRecruitClosed(RecruitClosedEvent event) {
		String message = event.seniorId() == null
				? "신청받을 사자가 탈퇴해서 모집이 마감됐어요. " + event.title()
				: "님이 여러 명 커피챗을 받지 않게 되어 모집이 마감됐어요. " + event.title();
		save(event.memberIds(), RECRUIT_CLOSED, event.postId(), event.seniorId(), message);
	}

	@EventListener
	public void onCommentCreated(CommentCreatedEvent event) {
		// 익명 댓글은 보낸 사람을 남기지 않고 문장 전체를 message에 쓴다.
		String prefix = event.commenterId() == null ? "익명" : "";
		String message = prefix + "님이 내 글에 댓글을 남겼어요. " + event.title();
		save(List.of(event.postAuthorId()), COMMENT_CREATED, event.postId(), event.commenterId(), message);
	}

	private void save(List<Long> userIds, String type, Long postId, Long actorId, String message) {
		notificationRepository.saveAll(userIds.stream()
				.map(userId -> Notification.builder()
						.userId(userId)
						.tab(NotificationTab.COMMUNITY)
						.type(type)
						.targetType(NotificationTargetType.POST)
						.targetId(postId)
						.actorId(actorId)
						.message(message)
						.build())
				.toList());
	}
}
