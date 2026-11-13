package com.lionlink.backend.domain.notification.service;

import com.lionlink.backend.domain.notification.dto.request.NotificationTabFilter;
import com.lionlink.backend.domain.notification.dto.response.NotificationActorResponse;
import com.lionlink.backend.domain.notification.dto.response.NotificationListResponse;
import com.lionlink.backend.domain.notification.dto.response.NotificationResponse;
import com.lionlink.backend.domain.notification.dto.response.UnreadCountResponse;
import com.lionlink.backend.domain.notification.entity.Notification;
import com.lionlink.backend.domain.notification.exception.NotificationErrorCode;
import com.lionlink.backend.domain.notification.repository.NotificationRepository;
import com.lionlink.backend.domain.profile.UserSummary;
import com.lionlink.backend.domain.profile.UserSummaryReader;
import com.lionlink.backend.global.error.BusinessException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final UserSummaryReader userSummaryReader;

	/** 최근 알림 20개와 탭별 안 읽은 개수. 보낸 사람 이름은 조회 시점의 이름으로 채운다. */
	@Transactional(readOnly = true)
	public NotificationListResponse getNotifications(Long userId, NotificationTabFilter filter) {
		List<Notification> notifications = filter.tab() == null
				? notificationRepository.findTop20ByUserIdOrderByCreatedAtDescIdDesc(userId)
				: notificationRepository.findTop20ByUserIdAndTabOrderByCreatedAtDescIdDesc(userId, filter.tab());
		Map<Long, UserSummary> actors = userSummaryReader.getSummaries(notifications.stream()
				.map(Notification::getActorId)
				.filter(Objects::nonNull)
				.distinct()
				.toList());
		List<NotificationResponse> items = notifications.stream()
				.map(notification -> NotificationResponse.of(notification, actor(actors, notification.getActorId())))
				.toList();
		return new NotificationListResponse(items,
				UnreadCountResponse.from(notificationRepository.countUnreadByTab(userId)));
	}

	/** 내 알림이 아니면 없는 알림으로 본다. 이미 읽은 알림이면 그대로 성공한다. */
	public void read(Long userId, Long notificationId) {
		Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
				.orElseThrow(() -> new BusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
		notification.read(LocalDateTime.now());
	}

	public void readAll(Long userId) {
		notificationRepository.readAll(userId, LocalDateTime.now());
	}

	private NotificationActorResponse actor(Map<Long, UserSummary> actors, Long actorId) {
		if (actorId == null) {
			return null;
		}
		return NotificationActorResponse.from(actors.getOrDefault(actorId, UserSummary.withdrawn(actorId)));
	}
}
