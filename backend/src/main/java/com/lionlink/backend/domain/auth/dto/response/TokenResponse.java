package com.lionlink.backend.domain.auth.dto.response;

/** 토큰 재발급(AUTH-03) 응답. 새 리프레시 토큰은 본문이 아니라 쿠키로 내려간다. */
public record TokenResponse(String accessToken, long expiresIn) {
}
