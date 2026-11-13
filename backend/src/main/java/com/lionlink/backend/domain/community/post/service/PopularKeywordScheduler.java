package com.lionlink.backend.domain.community.post.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PopularKeywordScheduler {

	private final PopularKeywordService popularKeywordService;

	@Scheduled(cron = "0 30 4 * * *")
	public void deleteExpired() {
		popularKeywordService.deleteExpired(LocalDateTime.now());
	}
}
