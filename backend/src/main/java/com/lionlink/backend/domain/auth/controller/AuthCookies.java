package com.lionlink.backend.domain.auth.controller;

import com.lionlink.backend.domain.auth.config.AuthProperties;
import com.lionlink.backend.global.config.WebConfig;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** 인증 쿠키는 모두 HttpOnly이고 /api/v1/auth 아래 요청에만 실린다. */
@Component
@RequiredArgsConstructor
public class AuthCookies {

	public static final String REFRESH_TOKEN = "refreshToken";
	public static final String SIGNUP_TOKEN = "signupToken";

	private final AuthProperties properties;

	public String refreshToken(String value) {
		return build(REFRESH_TOKEN, value, properties.refreshTokenTtl());
	}

	public String signupToken(String value) {
		return build(SIGNUP_TOKEN, value, properties.signupTokenTtl());
	}

	public String expireSignupToken() {
		return build(SIGNUP_TOKEN, "", Duration.ZERO);
	}

	public String expireRefreshToken() {
		return build(REFRESH_TOKEN, "", Duration.ZERO);
	}

	private String build(String name, String value, Duration maxAge) {
		return ResponseCookie.from(name, value)
				.httpOnly(true)
				.secure(properties.cookieSecure())
				.sameSite(properties.cookieSameSite())
				.path(WebConfig.API_PREFIX + "/auth")
				.maxAge(maxAge)
				.build()
				.toString();
	}
}
