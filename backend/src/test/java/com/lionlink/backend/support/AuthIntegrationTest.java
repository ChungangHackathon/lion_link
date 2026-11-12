package com.lionlink.backend.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.lionlink.backend.domain.auth.controller.AuthCookies;
import com.lionlink.backend.domain.auth.oauth.GoogleLoginSuccessHandler;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 통합 테스트 기반 클래스.
 * 구글과의 실제 왕복(로그인 화면·code 교환)은 테스트에서 재현할 수 없으므로,
 * 구글 사용자 정보로 로그인 성공 핸들러를 직접 호출해 "구글 로그인 직후" 상황을 만든다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class AuthIntegrationTest {

	protected static final String FRONTEND_URL = "http://localhost:3000";
	protected static final String REFRESH_COOKIE = AuthCookies.REFRESH_TOKEN;
	protected static final String SIGNUP_COOKIE = AuthCookies.SIGNUP_TOKEN;

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected EntityManager entityManager;

	@Autowired
	private GoogleLoginSuccessHandler googleLoginSuccessHandler;

	protected record Session(Long userId, String accessToken, String refreshToken) {
	}

	protected MockHttpServletResponse googleLogin(String sub, String email, String name, boolean emailVerified)
			throws Exception {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("sub", sub);
		attributes.put("email", email);
		attributes.put("name", name);
		attributes.put("email_verified", emailVerified);
		OAuth2User user = new DefaultOAuth2User(AuthorityUtils.createAuthorityList("OAUTH2_USER"), attributes, "sub");

		MockHttpServletResponse response = new MockHttpServletResponse();
		googleLoginSuccessHandler.onAuthenticationSuccess(new MockHttpServletRequest(), response,
				new OAuth2AuthenticationToken(user, user.getAuthorities(), "google"));
		return response;
	}

	protected MockHttpServletResponse googleLogin(String sub) throws Exception {
		return googleLogin(sub, sub + "@gmail.com", "Lion Kim", true);
	}

	/** 처음 온 구글 계정으로 로그인해 가입용 쿠키를 받는다. */
	protected Cookie signupCookie(String sub) throws Exception {
		return googleLogin(sub).getCookie(SIGNUP_COOKIE);
	}

	protected String json(String body) {
		return body.replace('\'', '"');
	}

	protected Session signup(String sub, String name, int generation) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
						.cookie(signupCookie(sub))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json("{'name':'" + name + "','generation':" + generation + ",'privacyAgreed':true}")))
				.andExpect(status().isCreated())
				.andReturn();
		String body = result.getResponse().getContentAsString();
		Number userId = JsonPath.read(body, "$.user.id");
		String accessToken = JsonPath.read(body, "$.accessToken");
		String refreshToken = result.getResponse().getCookie(REFRESH_COOKIE).getValue();
		return new Session(userId.longValue(), accessToken, refreshToken);
	}

	protected Session signup(String sub) throws Exception {
		return signup(sub, "김사자", 13);
	}

	protected String bearer(Session session) {
		return "Bearer " + session.accessToken();
	}

	protected void setStatus(Long userId, String status) {
		update("update users set status = :value where id = :id", userId, status);
	}

	protected void setRole(Long userId, String role) {
		update("update users set role = :value where id = :id", userId, role);
	}

	/** 관리자 반려(ADMIN-04) 결과를 재현한다. */
	protected void reject(Long userId, String reason) {
		setStatus(userId, "REJECTED");
		update("update users set reject_reason = :value where id = :id", userId, reason);
	}

	private void update(String sql, Long userId, String value) {
		entityManager.flush();
		entityManager.createNativeQuery(sql)
				.setParameter("value", value)
				.setParameter("id", userId)
				.executeUpdate();
		entityManager.clear();
	}
}
