package com.lionlink.backend.domain.auth.oauth;

import com.lionlink.backend.domain.auth.config.AuthProperties;
import com.lionlink.backend.domain.auth.exception.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/** 구글 로그인 실패를 {프론트}/login?error={코드}로 돌려보낸다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleLoginFailureHandler implements AuthenticationFailureHandler {

	/** 구글과 토큰·사용자 정보를 주고받다 실패한 경우 = 구글 쪽 문제. */
	private static final Set<String> GOOGLE_SIDE_ERRORS = Set.of("invalid_token_response", "invalid_user_info_response");

	private final AuthProperties properties;

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException {
		AuthErrorCode errorCode = AuthErrorCode.GOOGLE_AUTH_FAILED;
		if (exception instanceof OAuth2AuthenticationException oauth2Exception
				&& GOOGLE_SIDE_ERRORS.contains(oauth2Exception.getError().getErrorCode())) {
			errorCode = AuthErrorCode.GOOGLE_UNAVAILABLE;
		}
		log.info("Google login failed: {}", exception.getMessage());
		response.sendRedirect(loginErrorUrl(properties, errorCode));
	}

	static String loginErrorUrl(AuthProperties properties, AuthErrorCode errorCode) {
		return properties.frontendUrl() + "/login?error=" + errorCode.getCode();
	}
}
