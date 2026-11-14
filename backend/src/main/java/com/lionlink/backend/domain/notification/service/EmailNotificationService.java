package com.lionlink.backend.domain.notification.service;

import com.lionlink.backend.domain.notification.config.EmailProperties;
import com.lionlink.backend.domain.notification.email.EmailContent;
import com.lionlink.backend.domain.notification.email.EmailContentFactory;
import com.lionlink.backend.domain.notification.email.EmailSender;
import com.lionlink.backend.domain.notification.entity.EmailLog;
import com.lionlink.backend.domain.notification.entity.EmailStatus;
import com.lionlink.backend.domain.notification.entity.EmailType;
import com.lionlink.backend.domain.notification.entity.NotificationSetting;
import com.lionlink.backend.domain.notification.repository.EmailLogRepository;
import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 이메일 알림. 요청 처리 중에는 email_logs에 PENDING으로 쌓기만 하고, 실제 발송은 스케줄러가 맡는다.
 * 쌓기는 요청과 같은 트랜잭션이라 요청이 롤백되면 메일도 쌓이지 않고, 메일 서버가 느려도 API 응답은 기다리지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EmailNotificationService {

	private final EmailLogRepository emailLogRepository;
	private final UserRepository userRepository;
	private final NotificationSettingService notificationSettingService;
	private final EmailContentFactory emailContentFactory;
	private final EmailSender emailSender;
	private final EmailProperties properties;

	/**
	 * 메일을 보낼 목록에 넣는다. 받는 사람이 탈퇴했거나 대상이 없으면 넣지 않는다.
	 * 메시지 요청 메일은 채팅에서 요청을 만들 때 enqueue(받는 사람, MESSAGE_REQUEST, roomId)로 부르면 된다.
	 */
	public void enqueue(Long userId, EmailType type, Long targetId) {
		Optional<User> user = activeUser(userId);
		if (user.isEmpty()) {
			return;
		}
		emailContentFactory.render(type, targetId, "").ifPresent(content -> emailLogRepository.save(EmailLog.builder()
				.userId(userId)
				.toEmail(user.get().getEmail())
				.type(type)
				.targetIds(List.of(targetId))
				.subject(content.subject())
				.scheduledAt(LocalDateTime.now())
				.build()));
	}

	/** 보낼 시각이 된 메일을 보낸다. 몇 통을 처리했는지 돌려준다. */
	public int sendDue(LocalDateTime now) {
		List<EmailLog> logs = emailLogRepository.findDue(EmailStatus.PENDING, now, PageRequest.of(0, properties.batchSize()));
		logs.forEach(emailLog -> send(emailLog, now));
		return logs.size();
	}

	// 보내는 시점에 수신 설정과 대상을 다시 본다. 그사이 수신 거부했거나 신청이 처리됐으면 보내지 않는다.
	private void send(EmailLog emailLog, LocalDateTime now) {
		if (activeUser(emailLog.getUserId()).isEmpty()) {
			emailLog.skip();
			return;
		}
		NotificationSetting setting = notificationSettingService.getOrCreate(emailLog.getUserId());
		if (!setting.isEmailEnabled(emailLog.getType())) {
			emailLog.skip();
			return;
		}
		Optional<EmailContent> content = emailContentFactory.render(emailLog.getType(), emailLog.targetId(),
				unsubscribeUrl(setting.getUnsubscribeToken(), emailLog.getType()));
		if (content.isEmpty()) {
			emailLog.skip();
			return;
		}
		try {
			emailSender.send(emailLog.getToEmail(), content.get().subject(), content.get().html());
			emailLog.markSent(now);
		} catch (RuntimeException e) {
			// 속 원인까지 남긴다. 겉 메시지만으로는 주소 형식 오류인지 접속 실패인지 알 수 없다
			Throwable cause = e.getCause();
			log.warn("Email send failed. id={}, attempt={}, reason={}", emailLog.getId(), emailLog.getAttemptCount() + 1,
					cause == null ? e.getMessage() : e.getMessage() + ": " + cause.getMessage());
			emailLog.fail(now, properties.maxAttempts(), properties.retryDelay());
		}
	}

	private Optional<User> activeUser(Long userId) {
		return userRepository.findById(userId).filter(user -> user.getDeletedAt() == null);
	}

	/** 프론트 수신 거부 페이지(N-04). 페이지가 같은 값으로 NOTI-06을 부른다. */
	private String unsubscribeUrl(String token, EmailType type) {
		return UriComponentsBuilder.fromUriString(properties.linkBaseUrl())
				.path("/email-notifications/unsubscribe")
				.queryParam("token", token)
				.queryParam("type", type.name())
				.build()
				.toUriString();
	}
}
