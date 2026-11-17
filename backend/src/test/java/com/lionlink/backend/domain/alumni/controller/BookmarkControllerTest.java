package com.lionlink.backend.domain.alumni.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
class BookmarkControllerTest {

	private static final String USER_HEADER = DevHeaderCurrentUserProvider.HEADER;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EntityManager entityManager;

	private Long me;

	@BeforeEach
	void setUp() {
		me = TestUsers.approved(entityManager, "나사자");
	}

	@Test
	void 관심_동문을_저장한다() throws Exception {
		Long target = TestUsers.approved(entityManager, "김사자");

		mockMvc.perform(put("/api/v1/users/{userId}/bookmark", target).header(USER_HEADER, me))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(target))
				.andExpect(jsonPath("$.bookmarked").value(true));
	}

	@Test
	void 자기_자신을_저장하면_400() throws Exception {
		mockMvc.perform(put("/api/v1/users/{userId}/bookmark", me).header(USER_HEADER, me))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("CANNOT_BOOKMARK_SELF"))
				.andExpect(jsonPath("$.fieldErrors").doesNotExist());
	}

	@Test
	void 없는_회원을_저장하면_404() throws Exception {
		mockMvc.perform(put("/api/v1/users/{userId}/bookmark", 999_999L).header(USER_HEADER, me))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
	}

	@Test
	void 관심_동문을_취소하면_204() throws Exception {
		Long target = TestUsers.approved(entityManager, "김사자");
		mockMvc.perform(put("/api/v1/users/{userId}/bookmark", target).header(USER_HEADER, me));

		mockMvc.perform(delete("/api/v1/users/{userId}/bookmark", target).header(USER_HEADER, me))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
		mockMvc.perform(delete("/api/v1/users/{userId}/bookmark", target).header(USER_HEADER, me))
				.andExpect(status().isNoContent());
	}

	@Test
	void 관심_동문_목록을_동문_카드_형식으로_조회한다() throws Exception {
		Long target = TestUsers.approved(entityManager, "김사자");
		TestProfiles.save(entityManager, TestProfiles.profile(target).bio("서버 개발 이야기를 좋아해요"));
		mockMvc.perform(put("/api/v1/users/{userId}/bookmark", target).header(USER_HEADER, me));

		mockMvc.perform(get("/api/v1/users/me/bookmarks").header(USER_HEADER, me))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].id").value(target))
				.andExpect(jsonPath("$.items[0].name").value("김사자"))
				.andExpect(jsonPath("$.items[0].bio").value("서버 개발 이야기를 좋아해요"))
				.andExpect(jsonPath("$.items[0].bookmarked").value(true))
				.andExpect(jsonPath("$.items[0].coffeeChat.availability").value("NOT_ACCEPTING"))
				.andExpect(jsonPath("$.items[0].bookmarkedAt").value(endsWith("+09:00")))
				.andExpect(jsonPath("$.items[0].alumni").doesNotExist())
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(20))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.hasNext").value(false));
	}

	@Test
	void 페이지_크기가_범위를_벗어나면_400() throws Exception {
		mockMvc.perform(get("/api/v1/users/me/bookmarks").param("size", "101").header(USER_HEADER, me))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("size"))
				.andExpect(jsonPath("$.fieldErrors[0].message").value("페이지 크기는 1~100이어야 합니다."));
	}

	@Test
	void 로그인하지_않으면_401() throws Exception {
		mockMvc.perform(get("/api/v1/users/me/bookmarks"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void 승인되지_않은_회원은_403() throws Exception {
		Long pending = TestUsers.create(entityManager, "대기사자", UserStatus.PENDING);

		mockMvc.perform(get("/api/v1/users/me/bookmarks").header(USER_HEADER, pending))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("NOT_APPROVED"));
	}
}
