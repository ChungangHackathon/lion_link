package com.lionlink.backend.domain.notification.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 쌓인 메일을 10초마다 보낸다. 한 서버에서만 돈다고 보고 따로 잠그지 않는다. */
@Component
@RequiredArgsConstructor
public class EmailSendScheduler {

	private final EmailNotificationService emailNotificationService;

	@Scheduled(fixedDelayString = "${app.email.poll-interval-ms:10000}")
	public void sendDue() {
		emailNotificationService.sendDue(LocalDateTime.now());
	}
}
