package com.lionlink.backend.domain.auth.service;

import com.lionlink.backend.domain.auth.config.AuthProperties;
import com.lionlink.backend.domain.auth.entity.RefreshToken;
import com.lionlink.backend.domain.auth.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 리프레시 토큰 원문은 쿠키로만 나가고, DB에는 SHA-256 해시만 저장한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenService {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final RefreshTokenRepository refreshTokenRepository;
	private final AuthProperties properties;

	@Transactional
	public String issue(Long userId) {
		String raw = generate();
		refreshTokenRepository.save(RefreshToken.builder()
				.userId(userId)
				.tokenHash(hash(raw))
				.expiresAt(LocalDateTime.now().plus(properties.refreshTokenTtl()))
				.build());
		return raw;
	}

	private String generate() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hash(String raw) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
