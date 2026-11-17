package com.lionlink.backend.global.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
public class DevHeaderCurrentUserProvider implements CurrentUserProvider {

	public static final String HEADER = "X-User-Id";

	@Override
	public Optional<Long> findUserId(HttpServletRequest request) {
		String value = request.getHeader(HEADER);
		if (value == null || value.isBlank()) {
			return Optional.empty();
		}
		try {
			return Optional.of(Long.valueOf(value.trim()));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}
}
