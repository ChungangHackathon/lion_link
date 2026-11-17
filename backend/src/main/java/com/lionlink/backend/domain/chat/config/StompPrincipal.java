package com.lionlink.backend.domain.chat.config;

import java.security.Principal;

public record StompPrincipal(Long userId) implements Principal {

	@Override
	public String getName() {
		return String.valueOf(userId);
	}
}
