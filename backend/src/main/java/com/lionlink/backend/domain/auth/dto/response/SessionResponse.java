package com.lionlink.backend.domain.auth.dto.response;

/** 가입 신청(AUTH-02) 응답. */
public record SessionResponse(String accessToken, long expiresIn, MeResponse user) {
}
