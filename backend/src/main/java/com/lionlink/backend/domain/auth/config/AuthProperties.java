package com.lionlink.backend.domain.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
		String jwtSecret,
		@DefaultValue("http://localhost:3000") String frontendUrl,
		@DefaultValue("1h") Duration accessTokenTtl,
		@DefaultValue("14d") Duration refreshTokenTtl,
		@DefaultValue("10m") Duration signupTokenTtl,
		@DefaultValue("false") boolean cookieSecure,
		@DefaultValue("Lax") String cookieSameSite
) {
}
