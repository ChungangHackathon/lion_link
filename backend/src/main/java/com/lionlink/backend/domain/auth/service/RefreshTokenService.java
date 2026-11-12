package com.lionlink.backend.domain.auth.service;

import com.lionlink.backend.domain.auth.config.AuthProperties;
import com.lionlink.backend.domain.auth.entity.RefreshToken;
import com.lionlink.backend.domain.auth.repository.RefreshTokenRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.CommonErrorCode;
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

	/** 회전 결과: 토큰 주인과 쿠키로 내려줄 새 토큰 원문. */
	public record Rotated(Long userId, String refreshToken) {
	}

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

	/**
	 * AUTH-03: 받은 토큰을 폐기하고 같은 회원에게 새 토큰을 발급한다(회전).
	 * 이미 폐기된 토큰이 다시 오면 탈취로 보고 그 회원의 토큰을 전부 폐기한다.
	 * 그 폐기는 401을 던진 뒤에도 DB에 남아야 하므로 BusinessException에는 롤백하지 않는다.
	 */
	@Transactional(noRollbackFor = BusinessException.class)
	public Rotated rotate(String raw) {
		RefreshToken current = findOrUnauthenticated(raw);
		LocalDateTime now = LocalDateTime.now();
		if (current.isRevoked()) {
			refreshTokenRepository.revokeAllByUserId(current.getUserId(), now);
			throw unauthenticated();
		}
		if (current.isExpired(now)) {
			throw unauthenticated();
		}
		current.revoke(now);
		return new Rotated(current.getUserId(), issue(current.getUserId()));
	}

	/** AUTH-04: 토큰 하나를 폐기한다. 없거나 이미 폐기·만료된 토큰이어도 오류를 내지 않는다. */
	@Transactional
	public void revoke(String raw) {
		if (raw == null || raw.isBlank()) {
			return;
		}
		refreshTokenRepository.findByTokenHash(hash(raw))
				.ifPresent(token -> token.revoke(LocalDateTime.now()));
	}

	@Transactional
	public void revokeAll(Long userId) {
		refreshTokenRepository.revokeAllByUserId(userId, LocalDateTime.now());
	}

	private RefreshToken findOrUnauthenticated(String raw) {
		if (raw == null || raw.isBlank()) {
			throw unauthenticated();
		}
		return refreshTokenRepository.findByTokenHash(hash(raw)).orElseThrow(this::unauthenticated);
	}

	private BusinessException unauthenticated() {
		return new BusinessException(CommonErrorCode.UNAUTHENTICATED);
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
