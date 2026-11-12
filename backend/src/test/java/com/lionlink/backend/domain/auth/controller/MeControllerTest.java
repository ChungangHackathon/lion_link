package com.lionlink.backend.domain.auth.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.section.lionactivity.LionActivity;
import com.lionlink.backend.domain.profile.section.lionactivity.LionRole;
import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.support.AuthIntegrationTest;
import com.lionlink.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class MeControllerTest extends AuthIntegrationTest {

	private static final String SIGNUP_INFO = "/api/v1/me/signup-info";
	private static final String REAPPLY = "/api/v1/me/reapply";
	private static final String NEW_INFO = "{'name':'이사자','generation':12}";

	// ---------- AUTH-05 내 정보 조회 ----------

	@Test
	void 승인_대기_회원이_내_정보를_조회한다() throws Exception {
		Session lion = signup("lion");

		mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(lion)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(lion.userId()))
				.andExpect(jsonPath("$.name").value("김사자"))
				.andExpect(jsonPath("$.generation").value(13))
				.andExpect(jsonPath("$.email").value("lion@gmail.com"))
				.andExpect(jsonPath("$.role").value("MEMBER"))
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.profileCompleted").value(false))
				.andExpect(jsonPath("$.rejectReason").value(nullValue()))
				.andExpect(jsonPath("$.signupInfoEditable").value(true))
				.andExpect(jsonPath("$.photoUrl").value(nullValue()));
	}

	@Test
	void 반려된_회원은_반려_사유를_함께_받는다() throws Exception {
		Session lion = signup("lion");
		reject(lion.userId(), "기수 정보를 확인할 수 없어요.");

		mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(lion)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REJECTED"))
				.andExpect(jsonPath("$.rejectReason").value("기수 정보를 확인할 수 없어요."))
				.andExpect(jsonPath("$.signupInfoEditable").value(false));
	}

	@Test
	void 승인된_회원이_내_정보를_조회한다() throws Exception {
		Session lion = signup("lion");
		setStatus(lion.userId(), "APPROVED");

		mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(lion)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APPROVED"))
				.andExpect(jsonPath("$.signupInfoEditable").value(false));
	}

	@Test
	void 토큰이_없으면_내_정보_조회는_401() throws Exception {
		mockMvc.perform(get("/api/v1/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void 탈퇴한_회원의_남은_토큰으로_조회하면_401() throws Exception {
		Session lion = signup("lion");
		TestUsers.withdraw(entityManager, lion.userId());

		mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(lion)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	// ---------- AUTH-06 가입 정보 수정 ----------

	@Test
	void 승인_대기_회원이_가입_정보를_고친다() throws Exception {
		Session lion = signup("lion");

		perform(patch(SIGNUP_INFO), lion, "{'name':'  이사자 ','generation':12}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("이사자"))
				.andExpect(jsonPath("$.generation").value(12))
				.andExpect(jsonPath("$.status").value("PENDING"));

		User user = entityManager.find(User.class, lion.userId());
		assertEquals("이사자", user.getName());
		assertEquals(12, user.getGeneration());
	}

	@Test
	void 가입_정보를_고치면_자동_생성된_멋사_활동_기수도_바뀐다() throws Exception {
		Session lion = signup("lion");
		Long activityId = saveLionActivity(lion.userId(), 13, LionRole.LION, true);

		perform(patch(SIGNUP_INFO), lion, NEW_INFO)
				.andExpect(status().isOk());

		entityManager.flush();
		entityManager.clear();
		assertEquals(12, entityManager.find(LionActivity.class, activityId).getGeneration());
	}

	@Test
	void 바뀔_기수에_이미_멋사_활동이_있으면_409() throws Exception {
		Session lion = signup("lion");
		saveLionActivity(lion.userId(), 13, LionRole.LION, true);
		saveLionActivity(lion.userId(), 12, LionRole.STAFF, false);

		perform(patch(SIGNUP_INFO), lion, NEW_INFO)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE_ITEM"));
	}

	@Test
	void 반려된_회원이_가입_정보를_고치면_409_NOT_PENDING() throws Exception {
		Session lion = signup("lion");
		reject(lion.userId(), "기수 정보를 확인할 수 없어요.");

		perform(patch(SIGNUP_INFO), lion, NEW_INFO)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("NOT_PENDING"));
	}

	@Test
	void 승인된_회원이_가입_정보를_고치면_409_ALREADY_APPROVED() throws Exception {
		Session lion = signup("lion");
		setStatus(lion.userId(), "APPROVED");

		perform(patch(SIGNUP_INFO), lion, NEW_INFO)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ALREADY_APPROVED"));
	}

	@Test
	void 가입_정보가_범위를_벗어나면_400() throws Exception {
		Session lion = signup("lion");

		perform(patch(SIGNUP_INFO), lion, "{'name':'김','generation':0}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors", hasSize(2)));
	}

	@Test
	void 토큰이_없으면_가입_정보_수정은_401() throws Exception {
		mockMvc.perform(patch(SIGNUP_INFO)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(NEW_INFO)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	// ---------- AUTH-07 다시 신청 ----------

	@Test
	void 반려된_회원이_다시_신청하면_승인_대기로_돌아가고_반려_사유는_남는다() throws Exception {
		Session lion = signup("lion");
		reject(lion.userId(), "기수 정보를 확인할 수 없어요.");

		perform(post(REAPPLY), lion, NEW_INFO)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("이사자"))
				.andExpect(jsonPath("$.generation").value(12))
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.rejectReason").value(nullValue()))
				.andExpect(jsonPath("$.signupInfoEditable").value(true));

		entityManager.flush();
		entityManager.clear();
		User user = entityManager.find(User.class, lion.userId());
		assertNotNull(user.getReappliedAt());
		assertEquals("기수 정보를 확인할 수 없어요.", user.getRejectReason());
	}

	@Test
	void 다시_신청하면_관리자에게_가입_승인_요청_알림이_간다() throws Exception {
		Session admin = signup("admin");
		setRole(admin.userId(), "ADMIN");
		setStatus(admin.userId(), "APPROVED");
		Session lion = signup("lion");
		reject(lion.userId(), "기수 정보를 확인할 수 없어요.");

		perform(post(REAPPLY), lion, NEW_INFO)
				.andExpect(status().isOk());

		Number count = (Number) entityManager.createNativeQuery(
						"select count(*) from notifications where user_id = :admin and type = 'SIGNUP_REQUESTED' and target_id = :target")
				.setParameter("admin", admin.userId())
				.setParameter("target", lion.userId())
				.getSingleResult();
		// 가입 신청 1번 + 다시 신청 1번
		assertEquals(2, count.intValue());
	}

	@Test
	void 승인_대기_회원이_다시_신청하면_409_NOT_REJECTED() throws Exception {
		Session lion = signup("lion");

		perform(post(REAPPLY), lion, NEW_INFO)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("NOT_REJECTED"));
	}

	@Test
	void 승인된_회원이_다시_신청하면_409_NOT_REJECTED() throws Exception {
		Session lion = signup("lion");
		setStatus(lion.userId(), "APPROVED");

		perform(post(REAPPLY), lion, NEW_INFO)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("NOT_REJECTED"));
	}

	@Test
	void 다시_신청할_정보가_범위를_벗어나면_400() throws Exception {
		Session lion = signup("lion");
		reject(lion.userId(), "기수 정보를 확인할 수 없어요.");

		perform(post(REAPPLY), lion, "{'name':'','generation':15}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void 토큰이_없으면_다시_신청은_401() throws Exception {
		mockMvc.perform(post(REAPPLY)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(NEW_INFO)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	private ResultActions perform(
			MockHttpServletRequestBuilder request, Session session, String body) throws Exception {
		return mockMvc.perform(request
				.header(HttpHeaders.AUTHORIZATION, bearer(session))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(body)));
	}

	private Long saveLionActivity(Long userId, int generation, LionRole role, boolean isDefault) {
		LionActivity activity = LionActivity.builder()
				.userId(userId)
				.generation(generation)
				.role(role)
				.part1(LionPart.values()[0])
				.isDefault(isDefault)
				.build();
		entityManager.persist(activity);
		entityManager.flush();
		return activity.getId();
	}
}
