package com.lionlink.backend.domain.profile.basic.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionlink.backend.domain.profile.section.career.Career;
import com.lionlink.backend.domain.profile.section.certification.Certification;
import com.lionlink.backend.domain.profile.section.education.Education;
import com.lionlink.backend.domain.profile.section.language.Language;
import com.lionlink.backend.domain.profile.section.language.LanguageLevel;
import com.lionlink.backend.support.AuthIntegrationTest;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

class MyProfileControllerTest extends AuthIntegrationTest {

	private static final String STUDENT_BODY = "{'lionParts':['BACKEND'],'status':'STUDENT','interests':['BACKEND']}";

	@Test
	void 온보딩_전이면_403_PROFILE_NOT_COMPLETED() throws Exception {
		Session lion = signup("lion");

		getMyProfile(lion)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PROFILE_NOT_COMPLETED"));
	}

	@Test
	void 승인_대기_회원이_온보딩_직후_내_프로필을_조회한다() throws Exception {
		Session lion = signup("lion");
		onboard(lion, """
				{'lionParts':['FRONTEND','BACKEND'],'status':'EMPLOYED','interests':['BACKEND','DATA_AI'],
				 'company':'핀테크A','jobTitle':'백엔드 개발자','link':'https://github.com/lion','headline':'핀테크 백엔드 개발자'}""")
				.andExpect(status().isOk());

		getMyProfile(lion)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(lion.userId()))
				.andExpect(jsonPath("$.name").value("김사자"))
				.andExpect(jsonPath("$.generation").value(13))
				.andExpect(jsonPath("$.photoUrl").value(nullValue()))
				.andExpect(jsonPath("$.headline").value("핀테크 백엔드 개발자"))
				.andExpect(jsonPath("$.bio").value(nullValue()))
				.andExpect(jsonPath("$.status").value("EMPLOYED"))
				.andExpect(jsonPath("$.lionParts[0]").value("FRONTEND"))
				.andExpect(jsonPath("$.lionParts[1]").value("BACKEND"))
				.andExpect(jsonPath("$.interests", hasSize(2)))
				.andExpect(jsonPath("$.skills", hasSize(0)))
				.andExpect(jsonPath("$.links[0].type").value("GITHUB"))
				.andExpect(jsonPath("$.links[0].url").value("https://github.com/lion"))
				.andExpect(jsonPath("$.contact.email").value("lion@gmail.com"))
				.andExpect(jsonPath("$.contact.phone").value(nullValue()))
				.andExpect(jsonPath("$.visibility.email").value("CONNECTED"))
				.andExpect(jsonPath("$.visibility.phone").value("CONNECTED"))
				.andExpect(jsonPath("$.visibility.links").value("PUBLIC"))
				.andExpect(jsonPath("$.visibility.company").value("PUBLIC"))
				.andExpect(jsonPath("$.careers", hasSize(1)))
				.andExpect(jsonPath("$.careers[0].company").value("핀테크A"))
				.andExpect(jsonPath("$.careers[0].jobTitle").value("백엔드 개발자"))
				.andExpect(jsonPath("$.careers[0].isCurrent").value(true))
				.andExpect(jsonPath("$.careers[0].startYm").value(nullValue()))
				.andExpect(jsonPath("$.educations[0].school").value("중앙대학교"))
				.andExpect(jsonPath("$.lionActivities[0].generation").value(13))
				.andExpect(jsonPath("$.lionActivities[0].role").value("LION"))
				.andExpect(jsonPath("$.lionActivities[0].parts[1]").value("BACKEND"))
				.andExpect(jsonPath("$.lionActivities[0].isDefault").value(true))
				.andExpect(jsonPath("$.lionProjects", hasSize(0)))
				.andExpect(jsonPath("$.projects", hasSize(0)))
				.andExpect(jsonPath("$.certifications", hasSize(0)))
				.andExpect(jsonPath("$.awards", hasSize(0)))
				.andExpect(jsonPath("$.activities", hasSize(0)))
				.andExpect(jsonPath("$.languages", hasSize(0)));
	}

	@Test
	void 승인_회원도_내_프로필을_조회한다() throws Exception {
		Session lion = signup("lion");
		onboard(lion, STUDENT_BODY).andExpect(status().isOk());
		setStatus(lion.userId(), "APPROVED");

		getMyProfile(lion).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("STUDENT"));
	}

	@Test
	void 섹션은_진행_중인_항목이_맨_위고_나머지는_최신순이다() throws Exception {
		Session lion = signup("lion", "김사자", 13);
		onboard(lion, """
				{'lionParts':['BACKEND'],'status':'STUDENT','interests':['BACKEND'],
				 'staffActivities':[{'generation':10,'parts':['DESIGN']},{'generation':12,'parts':['PLANNING']}]}""")
				.andExpect(status().isOk());

		Long userId = lion.userId();
		entityManager.persist(career(userId, "오래된회사", YearMonth.of(2020, 1), YearMonth.of(2021, 1), false));
		entityManager.persist(career(userId, "최근회사", YearMonth.of(2022, 1), YearMonth.of(2023, 1), false));
		entityManager.persist(career(userId, "현재회사", YearMonth.of(2019, 5), null, true));
		entityManager.persist(Education.builder().userId(userId).school("대학원").startYm(YearMonth.of(2025, 3))
				.isCurrent(true).build());
		entityManager.persist(Certification.builder().userId(userId).name("정보처리기사").date(YearMonth.of(2023, 5)).build());
		entityManager.persist(Certification.builder().userId(userId).name("SQLD").date(YearMonth.of(2024, 9)).build());
		entityManager.persist(Language.builder().userId(userId).language("영어").level(LanguageLevel.BUSINESS).build());
		entityManager.persist(Language.builder().userId(userId).language("일본어").level(LanguageLevel.BASIC).build());
		entityManager.flush();
		entityManager.clear();

		getMyProfile(lion)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.careers[0].company").value("현재회사"))
				.andExpect(jsonPath("$.careers[1].company").value("최근회사"))
				.andExpect(jsonPath("$.careers[1].startYm").value("2022-01"))
				.andExpect(jsonPath("$.careers[2].company").value("오래된회사"))
				.andExpect(jsonPath("$.educations[0].school").value("대학원"))
				.andExpect(jsonPath("$.educations[1].school").value("중앙대학교"))
				.andExpect(jsonPath("$.lionActivities[0].generation").value(13))
				.andExpect(jsonPath("$.lionActivities[1].generation").value(12))
				.andExpect(jsonPath("$.lionActivities[2].generation").value(10))
				.andExpect(jsonPath("$.certifications[0].name").value("SQLD"))
				.andExpect(jsonPath("$.certifications[0].acquiredYm").value("2024-09"))
				.andExpect(jsonPath("$.certifications[1].name").value("정보처리기사"))
				.andExpect(jsonPath("$.languages[0].language").value("영어"))
				.andExpect(jsonPath("$.languages[1].language").value("일본어"));
	}

	@Test
	void 토큰이_없으면_내_프로필_조회는_401() throws Exception {
		mockMvc.perform(get("/api/v1/me/profile"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	private ResultActions getMyProfile(Session session) throws Exception {
		return mockMvc.perform(get("/api/v1/me/profile").header(HttpHeaders.AUTHORIZATION, bearer(session)));
	}

	private Career career(Long userId, String company, YearMonth start, YearMonth end, boolean current) {
		return Career.builder().userId(userId).company(company).title("개발자").startYm(start).endYm(end)
				.isCurrent(current).build();
	}
}
