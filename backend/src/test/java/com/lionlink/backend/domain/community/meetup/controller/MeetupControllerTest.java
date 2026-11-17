package com.lionlink.backend.domain.community.meetup.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionlink.backend.domain.community.post.entity.PostCategory;
import com.lionlink.backend.domain.community.post.service.PostService;
import com.lionlink.backend.global.security.DevHeaderCurrentUserProvider;
import com.lionlink.backend.support.TestPosts;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MeetupControllerTest {

	private static final String USER_HEADER = DevHeaderCurrentUserProvider.HEADER;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PostService postService;

	@Autowired
	private EntityManager entityManager;

	private Long host;
	private Long kim;
	private Long postId;

	@BeforeEach
	void setUp() {
		host = TestUsers.approved(entityManager, "나사자");
		kim = TestUsers.approved(entityManager, "김사자");
		postId = postService.create(host, TestPosts.meetup("금요일 번개", 2, List.of())).id();
	}

	@Test
	void 번개에_참여한다() throws Exception {
		mockMvc.perform(put("/api/v1/posts/{postId}/meetup/members/me", postId).header(USER_HEADER, kim))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.postId").value(postId))
				.andExpect(jsonPath("$.joined").value(true))
				.andExpect(jsonPath("$.participantCount").value(2))
				.andExpect(jsonPath("$.capacity").value(2))
				.andExpect(jsonPath("$.status").value("FULL"))
				.andExpect(jsonPath("$.members", hasSize(2)))
				.andExpect(jsonPath("$.members[0].isHost").value(true))
				.andExpect(jsonPath("$.members[1].name").value("김사자"));
	}

	@Test
	void 정원이_찬_번개에_참여하면_409() throws Exception {
		Long lee = TestUsers.approved(entityManager, "이사자");
		mockMvc.perform(put("/api/v1/posts/{postId}/meetup/members/me", postId).header(USER_HEADER, kim));

		mockMvc.perform(put("/api/v1/posts/{postId}/meetup/members/me", postId).header(USER_HEADER, lee))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("MEETUP_FULL"));
	}

	@Test
	void 번개가_아닌_게시글이면_404() throws Exception {
		Long freeId = postService.create(host, TestPosts.general(PostCategory.FREE, "자유 글")).id();

		mockMvc.perform(put("/api/v1/posts/{postId}/meetup/members/me", freeId).header(USER_HEADER, kim))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("MEETUP_NOT_FOUND"));
	}

	@Test
	void 참여를_취소한다() throws Exception {
		mockMvc.perform(put("/api/v1/posts/{postId}/meetup/members/me", postId).header(USER_HEADER, kim));

		mockMvc.perform(delete("/api/v1/posts/{postId}/meetup/members/me", postId).header(USER_HEADER, kim))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.joined").value(false))
				.andExpect(jsonPath("$.participantCount").value(1))
				.andExpect(jsonPath("$.status").value("OPEN"));
	}

	@Test
	void 작성자가_참여를_취소하면_403() throws Exception {
		mockMvc.perform(delete("/api/v1/posts/{postId}/meetup/members/me", postId).header(USER_HEADER, host))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("HOST_CANNOT_LEAVE"));
	}

	@Test
	void 참여하지_않은_번개를_취소하면_404() throws Exception {
		mockMvc.perform(delete("/api/v1/posts/{postId}/meetup/members/me", postId).header(USER_HEADER, kim))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_MEETUP_MEMBER"));
	}

	@Test
	void 동문을_초대한다() throws Exception {
		mockMvc.perform(post("/api/v1/posts/{postId}/meetup/invites", postId).header(USER_HEADER, host)
						.contentType(MediaType.APPLICATION_JSON).content("{\"userIds\": [" + kim + ", 999999]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sentCount").value(1))
				.andExpect(jsonPath("$.skippedUserIds[0]").value(999999));
	}

	@Test
	void 초대_대상이_없거나_20명을_넘으면_400() throws Exception {
		String tooMany = LongStream.rangeClosed(1, 21).mapToObj(String::valueOf).collect(Collectors.joining(","));

		mockMvc.perform(post("/api/v1/posts/{postId}/meetup/invites", postId).header(USER_HEADER, host)
						.contentType(MediaType.APPLICATION_JSON).content("{\"userIds\": []}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("userIds"));
		mockMvc.perform(post("/api/v1/posts/{postId}/meetup/invites", postId).header(USER_HEADER, host)
						.contentType(MediaType.APPLICATION_JSON).content("{\"userIds\": [" + tooMany + "]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].message").value("한 번에 최대 20명까지 초대할 수 있어요."));
	}

	@Test
	void 작성자가_아니면_초대할_수_없다() throws Exception {
		mockMvc.perform(post("/api/v1/posts/{postId}/meetup/invites", postId).header(USER_HEADER, kim)
						.contentType(MediaType.APPLICATION_JSON).content("{\"userIds\": [" + host + "]}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("NOT_OWNER"))
				.andExpect(jsonPath("$.message").value("번개모임 작성자만 초대할 수 있어요."));
	}
}
