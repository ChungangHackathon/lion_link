package com.lionlink.backend.domain.home.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.global.security.DevHeaderCurrentUserProvider;
import com.lionlink.backend.support.TestProfiles;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
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
class HomeControllerTest {

	private static final String USER_HEADER = DevHeaderCurrentUserProvider.HEADER;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EntityManager entityManager;

	private Long me;

	@BeforeEach
	void setUp() {
		me = TestUsers.approved(entityManager, "나사자");
		TestProfiles.save(entityManager, TestProfiles.profile(me).headline("백엔드 지망생"));
	}

	@Test
	void 홈_화면을_조회한다() throws Exception {
		completeProfile(me);

		mockMvc.perform(get("/api/v1/home").header(USER_HEADER, me))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.me.id").value(me))
				.andExpect(jsonPath("$.me.name").value("나사자"))
				.andExpect(jsonPath("$.me.lionParts[0]").value("BACKEND"))
				.andExpect(jsonPath("$.me.headline").value("백엔드 지망생"))
				.andExpect(jsonPath("$.welcomeBannerVisible").value(true))
				.andExpect(jsonPath("$.counts.pendingCoffeeChats").value(0))
				.andExpect(jsonPath("$.counts.activeCoffeeChats").value(0))
				.andExpect(jsonPath("$.counts.unreadMessages").value(0))
				.andExpect(jsonPath("$.counts.messageRequests").value(0))
				.andExpect(jsonPath("$.counts.bookmarks").value(0))
				.andExpect(jsonPath("$.upcomingCoffeeChat").value(nullValue()))
				.andExpect(jsonPath("$.recommendedSeniors").isArray())
				.andExpect(jsonPath("$.onThisDay").value(nullValue()));
	}

	@Test
	void 기본_프로필을_완성하지_않으면_403() throws Exception {
		mockMvc.perform(get("/api/v1/home").header(USER_HEADER, me))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PROFILE_NOT_COMPLETED"));
	}

	@Test
	void 승인되지_않은_회원은_403() throws Exception {
		Long pending = TestUsers.create(entityManager, "대기사자", UserStatus.PENDING);

		mockMvc.perform(get("/api/v1/home").header(USER_HEADER, pending))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("NOT_APPROVED"));
	}

	@Test
	void 환영_배너를_닫는다() throws Exception {
		mockMvc.perform(post("/api/v1/home/coffee-guide/dismiss").header(USER_HEADER, me))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.visible").value(false))
				.andExpect(jsonPath("$.dismissedAt").value(endsWith("+09:00")));

		completeProfile(me);
		mockMvc.perform(get("/api/v1/home").header(USER_HEADER, me))
				.andExpect(jsonPath("$.welcomeBannerVisible").value(false));
	}

	private void completeProfile(Long userId) {
		entityManager.flush();
		entityManager.createNativeQuery("update users set profile_completed = true where id = :id")
				.setParameter("id", userId)
				.executeUpdate();
		entityManager.clear();
	}
}
