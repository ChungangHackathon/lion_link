package com.lionlink.backend.domain.alumni.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionlink.backend.domain.coffeechat.entity.ReceiveMode;
import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.global.security.DevHeaderCurrentUserProvider;
import com.lionlink.backend.support.TestProfiles;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AlumniControllerTest {

	private static final String USER_HEADER = DevHeaderCurrentUserProvider.HEADER;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EntityManager entityManager;

	private Long me;

	@BeforeEach
	void setUp() {
		me = TestUsers.approved(entityManager, "나사자");
		TestProfiles.save(entityManager, TestProfiles.profile(me));
	}

	@Test
	void 동문을_검색한다() throws Exception {
		Long kim = TestUsers.approved(entityManager, "김사자");
		TestProfiles.save(entityManager, TestProfiles.profile(kim)
				.lionPart2(LionPart.FRONTEND)
				.skills(List.of("Spring", "MySQL"))
				.bio("서버 개발 이야기를 좋아해요"));
		TestProfiles.currentCareer(entityManager, kim, "예시회사", "백엔드 개발자");
		TestProfiles.coffeeSetting(entityManager, kim, ReceiveMode.ON, true);
		LocalDate today = LocalDate.now();
		TestProfiles.coffeePeriod(entityManager, kim, today, today.plusDays(3));

		mockMvc.perform(get("/api/v1/users")
						.param("q", "mysql")
						.param("generations", "13,14")
						.param("parts", "BACKEND")
						.header(USER_HEADER, me))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].id").value(kim))
				.andExpect(jsonPath("$.items[0].name").value("김사자"))
				.andExpect(jsonPath("$.items[0].generation").value(13))
				.andExpect(jsonPath("$.items[0].lionParts[0]").value("BACKEND"))
				.andExpect(jsonPath("$.items[0].status").value("EMPLOYED"))
				.andExpect(jsonPath("$.items[0].company").value("예시회사"))
				.andExpect(jsonPath("$.items[0].interests[0]").value("BACKEND"))
				.andExpect(jsonPath("$.items[0].skills[1]").value("MySQL"))
				.andExpect(jsonPath("$.items[0].bookmarked").value(false))
				.andExpect(jsonPath("$.items[0].bio").value("서버 개발 이야기를 좋아해요"))
				.andExpect(jsonPath("$.items[0].coffeeChat.availability").value("RESTING"))
				.andExpect(jsonPath("$.items[0].coffeeChat.nextAvailableDate").value(today.plusDays(4).toString()))
				.andExpect(jsonPath("$.items[0].coffeeChat.acceptGroup").value(true))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(20))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.hasNext").value(false));
	}

	@Test
	void 오늘_커피챗을_받는_동문만_조회한다() throws Exception {
		Long receiving = TestUsers.approved(entityManager, "받는사자");
		TestProfiles.save(entityManager, TestProfiles.profile(receiving));
		TestProfiles.coffeeSetting(entityManager, receiving, ReceiveMode.ON, true);
		Long other = TestUsers.approved(entityManager, "안받는사자");
		TestProfiles.save(entityManager, TestProfiles.profile(other));

		mockMvc.perform(get("/api/v1/users")
						.param("available", "true")
						.param("sort", "RECOMMEND")
						.header(USER_HEADER, me))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].id").value(receiving))
				.andExpect(jsonPath("$.items[0].coffeeChat.availability").value("AVAILABLE"))
				.andExpect(jsonPath("$.items[0].coffeeChat.nextAvailableDate").value(nullValue()))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void 없는_정렬이면_400() throws Exception {
		mockMvc.perform(get("/api/v1/users").param("sort", "POPULAR").header(USER_HEADER, me))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("sort"));
	}

	@Test
	void 검색_결과가_없으면_빈_목록() throws Exception {
		mockMvc.perform(get("/api/v1/users").param("q", "없는사람").header(USER_HEADER, me))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(0)))
				.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void 페이지_크기가_범위를_벗어나면_400() throws Exception {
		mockMvc.perform(get("/api/v1/users").param("size", "101").header(USER_HEADER, me))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
	}

	@Test
	void 없는_파트로_검색하면_400() throws Exception {
		mockMvc.perform(get("/api/v1/users").param("parts", "DEVOPS").header(USER_HEADER, me))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void 승인되지_않은_회원은_403() throws Exception {
		Long pending = TestUsers.create(entityManager, "대기사자", UserStatus.PENDING);

		mockMvc.perform(get("/api/v1/users").header(USER_HEADER, pending))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("NOT_APPROVED"));
	}

	@Test
	void 동문_프로필을_상세_조회한다() throws Exception {
		Long kim = TestUsers.approved(entityManager, "김사자");
		TestProfiles.save(entityManager, TestProfiles.profile(kim)
				.bio("서버 개발에 관심이 있습니다.")
				.contactEmail("kim@test.com"));
		TestProfiles.currentCareer(entityManager, kim, "예시회사", "백엔드 개발자");

		mockMvc.perform(get("/api/v1/users/{userId}", kim).header(USER_HEADER, me))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(kim))
				.andExpect(jsonPath("$.bio").value("서버 개발에 관심이 있습니다."))
				.andExpect(jsonPath("$.careers[0].company").value("예시회사"))
				.andExpect(jsonPath("$.careers[0].isCurrent").value(true))
				.andExpect(jsonPath("$.careers[0].startYm").value("2024-03"))
				.andExpect(jsonPath("$.educations", hasSize(0)))
				.andExpect(jsonPath("$.projects", hasSize(0)))
				.andExpect(jsonPath("$.lionActivities", hasSize(0)))
				.andExpect(jsonPath("$.contactEmail").value(nullValue()))
				.andExpect(jsonPath("$.phone").value(nullValue()))
				.andExpect(jsonPath("$.bookmarked").value(false))
				.andExpect(jsonPath("$.coffeeChatAvailable").value(false));
	}

	@Test
	void 없는_동문을_상세_조회하면_404() throws Exception {
		mockMvc.perform(get("/api/v1/users/{userId}", 999_999L).header(USER_HEADER, me))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("동문 정보를 찾을 수 없어요."));
	}

	@Test
	void 내_관심_동문_목록_경로와_겹치지_않는다() throws Exception {
		mockMvc.perform(get("/api/v1/users/me/bookmarks").header(USER_HEADER, me))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(0)));
	}
}
