package com.lionlink.backend.domain.coffeechat.service;

import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatStatus;
import com.lionlink.backend.domain.coffeechat.repository.CoffeeChatRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CoffeeChatCooldownPolicy {

	private static final int COOLDOWN_DAYS = 7;

	private final CoffeeChatRepository coffeeChatRepository;

	public Optional<LocalDate> availableFrom(Long requesterId, Long receiverId, LocalDate today) {
		LocalDateTime closedAt = coffeeChatRepository.findLastClosedAt(requesterId, receiverId,
				CoffeeChatStatus.REJECTED, CoffeeChatStatus.EXPIRED);
		if (closedAt == null) {
			return Optional.empty();
		}
		LocalDate availableDate = closedAt.toLocalDate().plusDays(COOLDOWN_DAYS);
		return availableDate.isAfter(today) ? Optional.of(availableDate) : Optional.empty();
	}
}
