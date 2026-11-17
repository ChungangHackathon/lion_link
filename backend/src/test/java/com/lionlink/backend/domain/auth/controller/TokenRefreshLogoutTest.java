package com.lionlink.backend.domain.auth.controller;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.lionlink.backend.support.AuthIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/** AUTH-03 토큰 재발급, AUTH-04 로그아웃. */
class TokenRefreshLogoutTest extends AuthIntegrationTest {

	private static final String REFRESH_URL = "/api/v1/auth/refresh";
	private static final String LOGOUT_URL = "/api/v1/auth/logout";

	@Test
	void 재발급하면_새_액세스_토큰과_회전된_리프레시_쿠키를_받는다() throws Exception {
		Session session = signup("lion");

		MvcResult result = refresh(session.refreshToken())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken", not(emptyOrNullString())))
				.andExpect(jsonPath("$.expiresIn").value(3600))
				.andExpect(cookie().httpOnly(REFRESH_COOKIE, true))
				.andExpect(cookie().path(REFRESH_COOKIE, "/api/v1/auth"))
				.andReturn();

		String newRefreshToken = result.getResponse().getCookie(REFRESH_COOKIE).getValue();
		assertNotEquals(session.refreshToken(), newRefreshToken);

		// 새 액세스 토큰으로 회원을 찾았기 때문에 401이 아니라 승인 전 403이 나온다.
		String accessToken = JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
		mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("NOT_APPROVED"));
	}

	@Test
	void 회전된_새_리프레시_토큰으로_다시_재발급할_수_있다() throws Exception {
		Session session = signup("lion");
		String second = refreshedToken(session.refreshToken());

		refresh(second).andExpect(status().isOk());
	}

	@Test
	void 구글_로그인으로_받은_리프레시_쿠키로_액세스_토큰을_받는다() throws Exception {
		signup("lion");
		Cookie refreshCookie = googleLogin("lion").getCookie(REFRESH_COOKIE);

		mockMvc.perform(post(REFRESH_URL).cookie(refreshCookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken", not(emptyOrNullString())));
	}

	@Test
	void 회전된_이전_토큰을_다시_쓰면_401이고_그_회원의_토큰이_모두_폐기된다() throws Exception {
		Session session = signup("lion");
		String rotated = refreshedToken(session.refreshToken());

		refresh(session.refreshToken())
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		// 정상 사용자가 가진 새 토큰도 함께 폐기돼 다시 로그인해야 한다.
		refresh(rotated).andExpect(status().isUnauthorized());
		assertEquals(0, aliveTokenCount(session.userId()));
	}

	@Test
	void 이전_토큰_재사용은_다른_회원의_토큰에_영향을_주지_않는다() throws Exception {
		Session lion = signup("lion");
		Session tiger = signup("tiger");
		refreshedToken(lion.refreshToken());

		refresh(lion.refreshToken()).andExpect(status().isUnauthorized());

		refresh(tiger.refreshToken()).andExpect(status().isOk());
	}

	@Test
	void 쿠키가_없으면_재발급은_401() throws Exception {
		mockMvc.perform(post(REFRESH_URL))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void 발급한_적_없는_토큰이면_재발급은_401() throws Exception {
		refresh("unknown-token")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void 만료된_토큰이면_재발급은_401() throws Exception {
		Session session = signup("lion");
		expireTokens(session.userId());

		refresh(session.refreshToken())
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void 탈퇴한_회원이면_재발급은_401이고_토큰이_폐기된다() throws Exception {
		Session session = signup("lion");
		withdraw(session.userId());

		refresh(session.refreshToken())
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
		assertEquals(0, aliveTokenCount(session.userId()));
	}

	@Test
	void 로그아웃하면_204이고_쿠키가_지워진다() throws Exception {
		Session session = signup("lion");

		mockMvc.perform(post(LOGOUT_URL).cookie(new Cookie(REFRESH_COOKIE, session.refreshToken())))
				.andExpect(status().isNoContent())
				.andExpect(cookie().maxAge(REFRESH_COOKIE, 0))
				.andExpect(cookie().path(REFRESH_COOKIE, "/api/v1/auth"));
	}

	@Test
	void 로그아웃한_토큰으로는_재발급할_수_없다() throws Exception {
		Session session = signup("lion");
		mockMvc.perform(post(LOGOUT_URL).cookie(new Cookie(REFRESH_COOKIE, session.refreshToken())))
				.andExpect(status().isNoContent());

		refresh(session.refreshToken())
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void 쿠키가_없거나_모르는_토큰이어도_로그아웃은_204() throws Exception {
		mockMvc.perform(post(LOGOUT_URL))
				.andExpect(status().isNoContent())
				.andExpect(cookie().maxAge(REFRESH_COOKIE, 0));
		mockMvc.perform(post(LOGOUT_URL).cookie(new Cookie(REFRESH_COOKIE, "unknown-token")))
				.andExpect(status().isNoContent());
	}

	@Test
	void 만료된_토큰으로_로그아웃해도_204() throws Exception {
		Session session = signup("lion");
		expireTokens(session.userId());

		mockMvc.perform(post(LOGOUT_URL).cookie(new Cookie(REFRESH_COOKIE, session.refreshToken())))
				.andExpect(status().isNoContent());
	}

	private ResultActions refresh(String refreshToken) throws Exception {
		return mockMvc.perform(post(REFRESH_URL).cookie(new Cookie(REFRESH_COOKIE, refreshToken)));
	}

	private String refreshedToken(String refreshToken) throws Exception {
		return refresh(refreshToken)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getCookie(REFRESH_COOKIE)
				.getValue();
	}

	private void expireTokens(Long userId) {
		updateTime("update refresh_tokens set expires_at = :time where user_id = :id", userId,
				LocalDateTime.now().minusMinutes(1));
	}

	private void withdraw(Long userId) {
		updateTime("update users set deleted_at = :time where id = :id", userId, LocalDateTime.now());
	}

	private void updateTime(String sql, Long userId, LocalDateTime time) {
		entityManager.flush();
		entityManager.createNativeQuery(sql)
				.setParameter("time", time)
				.setParameter("id", userId)
				.executeUpdate();
		entityManager.clear();
	}

	private int aliveTokenCount(Long userId) {
		entityManager.flush();
		Number count = (Number) entityManager.createNativeQuery(
						"select count(*) from refresh_tokens where user_id = :id and revoked_at is null")
				.setParameter("id", userId)
				.getSingleResult();
		return count.intValue();
	}
}
