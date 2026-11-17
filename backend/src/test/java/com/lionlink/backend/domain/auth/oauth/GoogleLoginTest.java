package com.lionlink.backend.domain.auth.oauth;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionlink.backend.support.AuthIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

class GoogleLoginTest extends AuthIntegrationTest {

	@Autowired
	private GoogleLoginFailureHandler failureHandler;

	@Test
	void 로그인_시작_주소는_구글_계정_선택_화면으로_보낸다() throws Exception {
		mockMvc.perform(get("/oauth2/authorization/google"))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string(HttpHeaders.LOCATION, startsWith("https://accounts.google.com/o/oauth2/v2/auth")))
				.andExpect(header().string(HttpHeaders.LOCATION, containsString("client_id=test-client-id")))
				.andExpect(header().string(HttpHeaders.LOCATION, containsString("prompt=select_account")));
	}

	@Test
	void 처음_로그인한_구글_계정은_가입용_쿠키를_받고_가입_화면으로_간다() throws Exception {
		MockHttpServletResponse response = googleLogin("lion");

		assertEquals(FRONTEND_URL + "/signup", response.getRedirectedUrl());
		Cookie signup = response.getCookie(SIGNUP_COOKIE);
		assertNotNull(signup);
		assertTrue(signup.isHttpOnly());
		assertEquals(600, signup.getMaxAge());
		assertEquals("/api/v1/auth", signup.getPath());
		assertNull(response.getCookie(REFRESH_COOKIE));
	}

	@Test
	void 가입한_계정은_리프레시_토큰_쿠키를_받고_콜백_화면으로_간다() throws Exception {
		signup("lion");

		MockHttpServletResponse response = googleLogin("lion");

		assertEquals(FRONTEND_URL + "/auth/callback", response.getRedirectedUrl());
		Cookie refresh = response.getCookie(REFRESH_COOKIE);
		assertNotNull(refresh);
		assertTrue(refresh.isHttpOnly());
		assertNull(response.getCookie(SIGNUP_COOKIE));
	}

	@Test
	void 토큰은_리다이렉트_주소에_싣지_않는다() throws Exception {
		assertEquals(-1, googleLogin("lion").getRedirectedUrl().indexOf('?'));
		signup("lion");
		assertEquals(-1, googleLogin("lion").getRedirectedUrl().indexOf('?'));
	}

	@Test
	void 이메일_인증이_안된_구글_계정은_로그인_실패로_보낸다() throws Exception {
		MockHttpServletResponse response = googleLogin("lion", "lion@gmail.com", "Lion Kim", false);

		assertEquals(FRONTEND_URL + "/login?error=GOOGLE_AUTH_FAILED", response.getRedirectedUrl());
		assertNull(response.getCookie(SIGNUP_COOKIE));
	}

	@Test
	void 구글과_토큰을_주고받다_실패하면_GOOGLE_UNAVAILABLE() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		failureHandler.onAuthenticationFailure(new MockHttpServletRequest(), response,
				new OAuth2AuthenticationException(new OAuth2Error("invalid_token_response")));

		assertEquals(FRONTEND_URL + "/login?error=GOOGLE_UNAVAILABLE", response.getRedirectedUrl());
	}

	@Test
	void 사용자가_로그인을_취소하는_등_그_밖의_실패는_GOOGLE_AUTH_FAILED() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		failureHandler.onAuthenticationFailure(new MockHttpServletRequest(), response,
				new OAuth2AuthenticationException(new OAuth2Error("access_denied")));

		assertEquals(FRONTEND_URL + "/login?error=GOOGLE_AUTH_FAILED", response.getRedirectedUrl());
	}
}
