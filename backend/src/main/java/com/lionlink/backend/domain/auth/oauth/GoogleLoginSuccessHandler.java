package com.lionlink.backend.domain.auth.oauth;

import com.lionlink.backend.domain.auth.config.AuthProperties;
import com.lionlink.backend.domain.auth.controller.AuthCookies;
import com.lionlink.backend.domain.auth.exception.AuthErrorCode;
import com.lionlink.backend.domain.auth.service.AuthService;
import com.lionlink.backend.domain.auth.service.GoogleIdentity;
import com.lionlink.backend.domain.auth.service.GoogleLoginResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * 구글 로그인 성공 후 프론트로 돌려보낸다. 토큰은 URL에 싣지 않고 HttpOnly 쿠키로만 준다.
 * <ul>
 *   <li>가입된 계정: refreshToken 쿠키 → {프론트}/auth/callback (프론트가 POST /auth/refresh로 accessToken을 받는다)</li>
 *   <li>처음 온 계정: signupToken 쿠키(10분) → {프론트}/signup</li>
 *   <li>이메일 미인증 계정: {프론트}/login?error=GOOGLE_AUTH_FAILED</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {

	static final String CALLBACK_PATH = "/auth/callback";
	static final String SIGNUP_PATH = "/signup";

	private final AuthService authService;
	private final AuthCookies authCookies;
	private final AuthProperties properties;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {
		invalidateSession(request);

		GoogleIdentity identity = authentication.getPrincipal() instanceof OAuth2User user
				? GoogleIdentity.from(user)
				: null;
		if (identity == null) {
			response.sendRedirect(GoogleLoginFailureHandler.loginErrorUrl(properties, AuthErrorCode.GOOGLE_AUTH_FAILED));
			return;
		}

		GoogleLoginResult result = authService.loginWithGoogle(identity);
		if (result.registered()) {
			response.addHeader(HttpHeaders.SET_COOKIE, authCookies.refreshToken(result.refreshToken()));
			response.sendRedirect(properties.frontendUrl() + CALLBACK_PATH);
			return;
		}
		response.addHeader(HttpHeaders.SET_COOKIE, authCookies.signupToken(result.signupToken()));
		response.sendRedirect(properties.frontendUrl() + SIGNUP_PATH);
	}

	/** 구글 왕복(state 보관)에만 쓴 세션을 지운다. 서비스 인증은 JWT로 한다. */
	private void invalidateSession(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
	}
}
