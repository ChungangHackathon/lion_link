package com.lionlink.backend.domain.community.post.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionlink.backend.domain.community.post.entity.PostCategory;
import com.lionlink.backend.domain.community.post.service.PostService;
import com.lionlink.backend.global.security.DevHeaderCurrentUserProvider;
import com.lionlink.backend.support.TestPosts;
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
class PopularKeywordControllerTest {

	private static final String USER_HEADER = DevHeaderCurrentUserProvider.HEADER;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PostService postService;

	@Autowired
	private EntityManager entityManager;

	private Long me;

	@BeforeEach
	void setUp() {
		me = TestUsers.approved(entityManager, "나사자");
		TestProfiles.save(entityManager, TestProfiles.profile(me));
		postService.create(me, TestPosts.general(PostCategory.FREE, "알고리즘 스터디 모집해요"));
	}

	@Test
	void 결과가_있는_첫_페이지_검색만_인기_키워드에_남는다() throws Exception {
		mockMvc.perform(get("/api/v1/posts").param("q", "알고리즘").header(USER_HEADER, me))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(1)));
		mockMvc.perform(get("/api/v1/posts").param("q", "없는검색어").header(USER_HEADER, me))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/posts").param("q", "스터디").param("page", "1").header(USER_HEADER, me))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/posts/popular-keywords").header(USER_HEADER, me))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].keyword").value("알고리즘"))
				.andExpect(jsonPath("$.items[0].count").value(1));
	}

	@Test
	void 개수가_범위를_벗어나면_400() throws Exception {
		mockMvc.perform(get("/api/v1/posts/popular-keywords").param("size", "21").header(USER_HEADER, me))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}
}
