package com.lionlink.backend.domain.auth.service;

/** 구글 로그인 결과. 가입된 계정이면 refreshToken, 처음 온 계정이면 signupToken이 있다. */
public record GoogleLoginResult(boolean registered, String refreshToken, String signupToken) {

	public static GoogleLoginResult registered(String refreshToken) {
		return new GoogleLoginResult(true, refreshToken, null);
	}

	public static GoogleLoginResult unregistered(String signupToken) {
		return new GoogleLoginResult(false, null, signupToken);
	}
}
