package com.lionlink.backend.domain.auth.service;

import com.lionlink.backend.domain.auth.config.AuthProperties;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.CommonErrorCode;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

	private static final String PURPOSE = "purpose";
	private static final String ACCESS = "access";
	private static final String SIGNUP = "signup";
	private static final String EMAIL = "email";
	private static final String NAME = "name";

	private final AuthProperties properties;
	private final JWSSigner signer;
	private final JWSVerifier verifier;

	public JwtProvider(AuthProperties properties) {
		this.properties = properties;
		String secret = properties.jwtSecret();
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalStateException("app.auth.jwt-secret 은 32바이트 이상이어야 해요.");
		}
		byte[] key = secret.getBytes(StandardCharsets.UTF_8);
		try {
			this.signer = new MACSigner(key);
			this.verifier = new MACVerifier(key);
		} catch (JOSEException e) {
			throw new IllegalStateException("JWT 서명 키를 만들 수 없어요.", e);
		}
	}

	public long accessTokenExpiresInSeconds() {
		return properties.accessTokenTtl().toSeconds();
	}

	public String createAccessToken(Long userId) {
		JWTClaimsSet claims = baseClaims(String.valueOf(userId), ACCESS, properties.accessTokenTtl()).build();
		return sign(claims);
	}

	/** 유효한 액세스 토큰이면 회원 ID, 아니면 UNAUTHENTICATED. */
	public Long parseAccessToken(String token) {
		JWTClaimsSet claims = parse(token, ACCESS);
		try {
			return Long.valueOf(claims.getSubject());
		} catch (NumberFormatException e) {
			throw unauthenticated();
		}
	}

	public String createSignupToken(GoogleIdentity identity) {
		JWTClaimsSet.Builder builder = baseClaims(identity.sub(), SIGNUP, properties.signupTokenTtl())
				.claim(EMAIL, identity.email());
		if (identity.name() != null) {
			builder.claim(NAME, identity.name());
		}
		return sign(builder.build());
	}

	public GoogleIdentity parseSignupToken(String token) {
		JWTClaimsSet claims = parse(token, SIGNUP);
		try {
			return new GoogleIdentity(claims.getSubject(), claims.getStringClaim(EMAIL), claims.getStringClaim(NAME));
		} catch (ParseException e) {
			throw unauthenticated();
		}
	}

	private JWTClaimsSet.Builder baseClaims(String subject, String purpose, Duration ttl) {
		Instant now = Instant.now();
		return new JWTClaimsSet.Builder()
				.subject(subject)
				.claim(PURPOSE, purpose)
				.issueTime(Date.from(now))
				.expirationTime(Date.from(now.plus(ttl)));
	}

	private String sign(JWTClaimsSet claims) {
		try {
			SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
			jwt.sign(signer);
			return jwt.serialize();
		} catch (JOSEException e) {
			throw new IllegalStateException("JWT 서명에 실패했어요.", e);
		}
	}

	private JWTClaimsSet parse(String token, String expectedPurpose) {
		if (token == null || token.isBlank()) {
			throw unauthenticated();
		}
		try {
			SignedJWT jwt = SignedJWT.parse(token);
			if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm()) || !jwt.verify(verifier)) {
				throw unauthenticated();
			}
			JWTClaimsSet claims = jwt.getJWTClaimsSet();
			Date expiration = claims.getExpirationTime();
			if (expiration == null || expiration.toInstant().isBefore(Instant.now())) {
				throw unauthenticated();
			}
			if (!expectedPurpose.equals(claims.getStringClaim(PURPOSE)) || claims.getSubject() == null) {
				throw unauthenticated();
			}
			return claims;
		} catch (ParseException | JOSEException e) {
			throw unauthenticated();
		}
	}

	private BusinessException unauthenticated() {
		return new BusinessException(CommonErrorCode.UNAUTHENTICATED);
	}
}
