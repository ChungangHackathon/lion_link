package com.lionlink.backend.domain.auth.controller;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionlink.backend.support.AuthIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class AuthControllerTest extends AuthIntegrationTest {

	private static final String SIGNUP_BODY = "{'name':'김사자','generation':13,'privacyAgreed':true}";

	@Test
	void 가입_화면에서_구글_이메일과_이름을_조회한다() throws Exception {
		Cookie signupCookie = googleLogin("lion", "lion@gmail.com", "Lion Kim", true).getCookie(SIGNUP_COOKIE);

		mockMvc.perform(get("/api/v1/auth/signup/google-info").cookie(signupCookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("lion@gmail.com"))
				.andExpect(jsonPath("$.name").value("Lion Kim"));
	}

	@Test
	void 가입용_쿠키가_없으면_구글_정보_조회는_401() throws Exception {
		mockMvc.perform(get("/api/v1/auth/signup/google-info"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void 가입하면_승인_대기_상태로_바로_로그인된다() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
						.cookie(signupCookie("lion"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(SIGNUP_BODY)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.accessToken", not(emptyOrNullString())))
				.andExpect(jsonPath("$.expiresIn").value(3600))
				.andExpect(jsonPath("$.user.name").value("김사자"))
				.andExpect(jsonPath("$.user.generation").value(13))
				.andExpect(jsonPath("$.user.email").value("lion@gmail.com"))
				.andExpect(jsonPath("$.user.role").value("MEMBER"))
				.andExpect(jsonPath("$.user.status").value("PENDING"))
				.andExpect(jsonPath("$.user.profileCompleted").value(false))
				.andExpect(jsonPath("$.user.signupInfoEditable").value(true))
				.andExpect(cookie().httpOnly(REFRESH_COOKIE, true))
				.andExpect(cookie().maxAge(SIGNUP_COOKIE, 0));
	}

	@Test
	void 개인정보_동의를_하지_않으면_400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
						.cookie(signupCookie("lion"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json("{'name':'김사자','generation':13,'privacyAgreed':false}")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("privacyAgreed"));
	}

	@Test
	void 이름과_기수가_범위를_벗어나면_400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
						.cookie(signupCookie("lion"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json("{'name':'김','generation':15,'privacyAgreed':true}")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors", hasSize(2)));
	}

	@Test
	void 가입용_쿠키가_없으면_가입_신청은_401() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(SIGNUP_BODY)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void 위조된_가입용_쿠키는_401() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
						.cookie(new Cookie(SIGNUP_COOKIE, "not-a-token"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(SIGNUP_BODY)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 액세스_토큰으로는_가입_신청을_할_수_없다() throws Exception {
		Session session = signup("someone");

		mockMvc.perform(post("/api/v1/auth/signup")
						.cookie(new Cookie(SIGNUP_COOKIE, session.accessToken()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(SIGNUP_BODY)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 같은_가입용_쿠키로_두_번_신청하면_409() throws Exception {
		Cookie signupCookie = signupCookie("lion");

		mockMvc.perform(post("/api/v1/auth/signup").cookie(signupCookie)
						.contentType(MediaType.APPLICATION_JSON).content(json(SIGNUP_BODY)))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/v1/auth/signup").cookie(signupCookie)
						.contentType(MediaType.APPLICATION_JSON).content(json(SIGNUP_BODY)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ALREADY_REGISTERED"));
	}

	@Test
	void 가입_신청이_들어오면_관리자에게_알림이_간다() throws Exception {
		Session admin = signup("admin");
		setRole(admin.userId(), "ADMIN");
		setStatus(admin.userId(), "APPROVED");

		Session lion = signup("lion");

		Number count = (Number) entityManager.createNativeQuery(
						"select count(*) from notifications where user_id = :admin and type = 'SIGNUP_REQUESTED' and target_id = :target")
				.setParameter("admin", admin.userId())
				.setParameter("target", lion.userId())
				.getSingleResult();
		assertEquals(1, count.intValue());
	}

	@Test
	void 발급한_액세스_토큰으로_회원을_식별한다() throws Exception {
		Session session = signup("lion");

		// 토큰으로 회원을 찾았기 때문에 401이 아니라, 승인 전 회원이라는 403이 나온다.
		mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, bearer(session)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("NOT_APPROVED"));
	}

	@Test
	void 잘못된_액세스_토큰은_401() throws Exception {
		mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, "Bearer wrong"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}
}
