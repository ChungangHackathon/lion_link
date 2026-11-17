package com.lionlink.backend.domain.coffeechat.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CoffeeChatScheduler {

	private final CoffeeChatService coffeeChatService;

	@Scheduled(cron = "0 0 * * * *")
	public void closeOverdue() {
		LocalDateTime now = LocalDateTime.now();
		coffeeChatService.expireOverdue(now);
		coffeeChatService.completeOverdue(now);
	}
}
