package com.lionlink.backend.domain.coffeechat.dto.response;

import com.lionlink.backend.domain.coffeechat.entity.CoffeeChat;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatStatus;
import java.time.LocalDateTime;

public record CoffeeChatCreateResponse(Long id, CoffeeChatStatus status, LocalDateTime createdAt, LocalDateTime expiresAt) {

	public static CoffeeChatCreateResponse from(CoffeeChat coffeeChat) {
		return new CoffeeChatCreateResponse(coffeeChat.getId(), coffeeChat.getStatus(), coffeeChat.getCreatedAt(),
				coffeeChat.getExpiresAt());
	}
}
