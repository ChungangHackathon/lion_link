package com.lionlink.backend.domain.auth.service;

/** 응답 본문과, 쿠키로 내려줄 리프레시 토큰(없으면 null). */
public record Issued<T>(T body, String refreshToken) {
}
