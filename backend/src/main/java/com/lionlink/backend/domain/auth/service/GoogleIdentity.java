package com.lionlink.backend.domain.auth.service;

import org.springframework.security.oauth2.core.user.OAuth2User;

/** 구글 로그인으로 확인한 계정 정보. sub는 구글 계정의 고유 ID다. */
public record GoogleIdentity(String sub, String email, String name) {

	/** 이메일 인증이 안 된 계정이거나 필수 값이 없으면 null. */
	public static GoogleIdentity from(OAuth2User user) {
		String sub = user.getAttribute("sub");
		String email = user.getAttribute("email");
		Object emailVerified = user.getAttribute("email_verified");
		boolean verified = Boolean.TRUE.equals(emailVerified) || "true".equals(String.valueOf(emailVerified));
		if (sub == null || email == null || !verified) {
			return null;
		}
		return new GoogleIdentity(sub, email, user.getAttribute("name"));
	}
}
