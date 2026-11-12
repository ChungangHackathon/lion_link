package com.lionlink.backend.domain.community.recruit.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionlink.backend.domain.community.post.entity.PostCategory;
import com.lionlink.backend.domain.community.post.service.PostService;
import com.lionlink.backend.global.security.DevHeaderCurrentUserProvider;
import com.lionlink.backend.support.TestPosts;
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
class RecruitControllerTest {

	private static final String USER_HEADER = DevHeaderCurrentUserProvider.HEADER;
	private static final String JOIN_URL = "/api/v1/posts/{postId}/recruit/members/me";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PostService postService;

	@Autowired
	private EntityManager entityManager;

	private Long me;
	private Long senior;
	private Long kim;
	private Long postId;

	@BeforeEach
	void setUp() {
		me = TestUsers.approved(entityManager, "나사자");
		senior = TestUsers.approved(entityManager, "선배사자");
		kim = TestUsers.approved(entityManager, "김사자");
		TestPosts.acceptGroupCoffeeChat(entityManager, senior);
		postId = postService.create(me, TestPosts.recruit("함께 커피챗 신청해요", senior, 2)).id();
	}

	@Test
	void 커피챗_모집에_참여한다() throws Exception {
		mockMvc.perform(put(JOIN_URL, postId).header(USER_HEADER, kim))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.postId").value(postId))
				.andExpect(jsonPath("$.joined").value(true))
				.andExpect(jsonPath("$.memberCount").value(2))
				.andExpect(jsonPath("$.capacity").value(2))
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$.members", hasSize(2)))
				.andExpect(jsonPath("$.members[0].id").value(me))
				.andExpect(jsonPath("$.members[0].isHost").value(true))
				.andExpect(jsonPath("$.members[1].name").value("김사자"));
	}

	@Test
	void 신청받을_사자가_참여하면_403() throws Exception {
		mockMvc.perform(put(JOIN_URL, postId).header(USER_HEADER, senior))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("SENIOR_CANNOT_JOIN"));
	}

	@Test
	void 정원이_찼으면_409() throws Exception {
		mockMvc.perform(put(JOIN_URL, postId).header(USER_HEADER, kim));
		Long lee = TestUsers.approved(entityManager, "이사자");

		mockMvc.perform(put(JOIN_URL, postId).header(USER_HEADER, lee))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("RECRUIT_FULL"))
				.andExpect(jsonPath("$.message").value("모집 인원이 마감되었어요."));
	}

	@Test
	void 커피챗_모집_글이_아니면_404() throws Exception {
		Long general = postService.create(me, TestPosts.general(PostCategory.FREE, "자유 글")).id();

		mockMvc.perform(put(JOIN_URL, general).header(USER_HEADER, kim))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RECRUIT_NOT_FOUND"));
	}

	@Test
	void 로그인하지_않으면_401() throws Exception {
		mockMvc.perform(put(JOIN_URL, postId))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}
}
