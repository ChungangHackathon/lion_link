package com.lionlink.backend.domain.auth.service;

import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.security.CurrentUserProvider;
import com.lionlink.backend.global.security.DevHeaderCurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * Authorization: Bearer 토큰에서 회원을 찾는다.
 * 헤더가 없으면 개발용 X-User-Id 헤더(prod 제외)로 넘어간다.
 */
@Primary
@Component
@RequiredArgsConstructor
public class JwtCurrentUserProvider implements CurrentUserProvider {

	private static final String BEARER = "Bearer ";

	private final JwtProvider jwtProvider;
	private final ObjectProvider<DevHeaderCurrentUserProvider> devProvider;

	@Override
	public Optional<Long> findUserId(HttpServletRequest request) {
		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authorization == null || authorization.isBlank()) {
			DevHeaderCurrentUserProvider dev = devProvider.getIfAvailable();
			return dev == null ? Optional.empty() : dev.findUserId(request);
		}
		if (!authorization.startsWith(BEARER)) {
			return Optional.empty();
		}
		try {
			return Optional.of(jwtProvider.parseAccessToken(authorization.substring(BEARER.length()).trim()));
		} catch (BusinessException e) {
			return Optional.empty();
		}
	}
}
