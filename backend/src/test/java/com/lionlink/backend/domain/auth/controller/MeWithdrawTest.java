package com.lionlink.backend.domain.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionlink.backend.domain.user.event.UserWithdrawnEvent;
import com.lionlink.backend.support.AuthIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.ResultActions;

@RecordApplicationEvents
class MeWithdrawTest extends AuthIntegrationTest {

	private static final String WITHDRAW_URL = "/api/v1/me/withdraw";

	@Autowired
	private ApplicationEvents events;

	@Test
	void 탈퇴하면_204와_함께_토큰이_폐기되고_쿠키가_지워지고_탈퇴_정보가_기록된다() throws Exception {
		Session session = signup("lion");
		setStatus(session.userId(), "APPROVED");

		withdraw(session, "{'confirm':'탈퇴'}")
				.andExpect(status().isNoContent())
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(REFRESH_COOKIE + "=")))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

		Object[] row = userRow(session.userId());
		assertThat(row[0]).as("deleted_at").isNotNull();
		assertThat(row[1]).as("google_sub").isNull();
		assertThat(events.stream(UserWithdrawnEvent.class))
				.extracting(UserWithdrawnEvent::userId)
				.containsExactly(session.userId());
		mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, session.refreshToken())))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 확인_문구가_정확히_탈퇴가_아니면_400이고_탈퇴되지_않는다() throws Exception {
		Session session = signup("lion");

		for (String body : new String[] {"{'confirm':'탈퇴 '}", "{'confirm':'탈퇴합니다'}", "{'confirm':''}", "{}"}) {
			withdraw(session, body)
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
					.andExpect(jsonPath("$.fieldErrors[0].field").value("confirm"));
		}

		assertThat(userRow(session.userId())[0]).isNull();
		assertThat(events.stream(UserWithdrawnEvent.class)).isEmpty();
	}

	@Test
	void 로그인하지_않으면_401() throws Exception {
		mockMvc.perform(post(WITHDRAW_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json("{'confirm':'탈퇴'}")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void 관리자는_탈퇴할_수_없다() throws Exception {
		Session session = signup("admin");
		setStatus(session.userId(), "APPROVED");
		setRole(session.userId(), "ADMIN");

		withdraw(session, "{'confirm':'탈퇴'}")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ADMIN_CANNOT_WITHDRAW"));

		assertThat(userRow(session.userId())[0]).isNull();
		assertThat(events.stream(UserWithdrawnEvent.class)).isEmpty();
	}

	@Test
	void 승인_대기_회원도_탈퇴할_수_있다() throws Exception {
		Session session = signup("pending");

		withdraw(session, "{'confirm':'탈퇴'}").andExpect(status().isNoContent());

		assertThat(userRow(session.userId())[0]).isNotNull();
	}

	@Test
	void 반려_회원도_탈퇴할_수_있다() throws Exception {
		Session session = signup("rejected");
		reject(session.userId(), "기수를 확인할 수 없어요.");

		withdraw(session, "{'confirm':'탈퇴'}").andExpect(status().isNoContent());

		assertThat(userRow(session.userId())[0]).isNotNull();
	}

	@Test
	void 탈퇴한_뒤_남은_액세스_토큰은_어느_API에서도_401이다() throws Exception {
		Session session = signup("lion");
		setStatus(session.userId(), "APPROVED");
		withdraw(session, "{'confirm':'탈퇴'}").andExpect(status().isNoContent());

		// 승인 검사를 건너뛰는 경로: 서비스가 직접 막는다
		mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(session)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
		// 인터셉터를 타는 경로
		mockMvc.perform(get("/api/v1/me/profile").header(HttpHeaders.AUTHORIZATION, bearer(session)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
		// 이미 탈퇴한 회원이 다시 탈퇴
		withdraw(session, "{'confirm':'탈퇴'}")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
		assertThat(events.stream(UserWithdrawnEvent.class)).hasSize(1);
	}

	@Test
	void 탈퇴한_구글_계정으로_다시_로그인하면_가입_흐름으로_가고_재가입하면_새_회원이_생긴다() throws Exception {
		Session first = signup("lion");
		withdraw(first, "{'confirm':'탈퇴'}").andExpect(status().isNoContent());

		var response = googleLogin("lion");
		assertThat(response.getCookie(SIGNUP_COOKIE)).isNotNull();
		assertThat(response.getCookie(REFRESH_COOKIE)).isNull();

		Session second = signup("lion", "김사자", 13);
		assertThat(second.userId()).isNotEqualTo(first.userId());
		assertThat(userRow(first.userId())[0]).isNotNull();
		assertThat(userRow(second.userId())[0]).isNull();
		assertThat(userRow(second.userId())[1]).isEqualTo("lion");
	}

	private ResultActions withdraw(Session session, String body) throws Exception {
		return mockMvc.perform(post(WITHDRAW_URL)
				.header(HttpHeaders.AUTHORIZATION, bearer(session))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(body)));
	}

	/** [deleted_at, google_sub] */
	private Object[] userRow(Long userId) {
		entityManager.flush();
		entityManager.clear();
		return (Object[]) entityManager
				.createNativeQuery("select deleted_at, google_sub from users where id = :id")
				.setParameter("id", userId)
				.getSingleResult();
	}
}
