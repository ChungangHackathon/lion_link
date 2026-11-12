package com.lionlink.backend.domain.community.comment.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionlink.backend.domain.community.comment.dto.request.CommentCreateRequest;
import com.lionlink.backend.domain.community.comment.service.CommentService;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommentControllerTest {

	private static final String USER_HEADER = DevHeaderCurrentUserProvider.HEADER;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PostService postService;

	@Autowired
	private CommentService commentService;

	@Autowired
	private EntityManager entityManager;

	private Long me;
	private Long postId;

	@BeforeEach
	void setUp() {
		me = TestUsers.approved(entityManager, "나사자");
		TestProfiles.save(entityManager, TestProfiles.profile(me));
		postId = postService.create(me, TestPosts.general(PostCategory.FREE, "자유 글")).id();
	}

	@Test
	void 댓글을_작성하면_201() throws Exception {
		mockMvc.perform(post("/api/v1/posts/{postId}/comments", postId).header(USER_HEADER, me)
						.contentType(MediaType.APPLICATION_JSON).content("{\"content\": \"좋은 글이에요\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.postId").value(postId))
				.andExpect(jsonPath("$.content").value("좋은 글이에요"))
				.andExpect(jsonPath("$.isAnonymous").value(false))
				.andExpect(jsonPath("$.author.name").value("나사자"))
				.andExpect(jsonPath("$.author.generation").value(13))
				.andExpect(jsonPath("$.author.lionParts[0]").value("BACKEND"))
				.andExpect(jsonPath("$.isMine").value(true))
				.andExpect(jsonPath("$.createdAt").value(endsWith("+09:00")));
	}

	@Test
	void 댓글이_비었거나_500자를_넘으면_400() throws Exception {
		mockMvc.perform(post("/api/v1/posts/{postId}/comments", postId).header(USER_HEADER, me)
						.contentType(MediaType.APPLICATION_JSON).content("{\"content\": \"   \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("content"));
		mockMvc.perform(post("/api/v1/posts/{postId}/comments", postId).header(USER_HEADER, me)
						.contentType(MediaType.APPLICATION_JSON).content("{\"content\": \"" + "가".repeat(501) + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].message").value("댓글은 1~500자여야 합니다."));
	}

	@Test
	void 댓글_목록을_조회한다() throws Exception {
		Long deleted = commentService.create(me, postId, new CommentCreateRequest("지울 댓글")).id();
		commentService.create(me, postId, new CommentCreateRequest("남길 댓글"));
		commentService.delete(me, postId, deleted);

		mockMvc.perform(get("/api/v1/posts/{postId}/comments", postId).header(USER_HEADER, me))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(2)))
				.andExpect(jsonPath("$.items[0].deleted").value(true))
				.andExpect(jsonPath("$.items[0].content").value("삭제된 댓글이에요."))
				.andExpect(jsonPath("$.items[0].author").value(nullValue()))
				.andExpect(jsonPath("$.items[1].content").value("남길 댓글"))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(20))
				.andExpect(jsonPath("$.hasNext").value(false))
				.andExpect(jsonPath("$.totalElements").doesNotExist());
	}

	@Test
	void 없는_게시글의_댓글은_404() throws Exception {
		mockMvc.perform(get("/api/v1/posts/{postId}/comments", 999_999L).header(USER_HEADER, me))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
	}

	@Test
	void 댓글을_삭제하면_204() throws Exception {
		Long commentId = commentService.create(me, postId, new CommentCreateRequest("댓글")).id();

		mockMvc.perform(delete("/api/v1/posts/{postId}/comments/{commentId}", postId, commentId).header(USER_HEADER, me))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
	}

	@Test
	void 다른_사람의_댓글을_삭제하면_403() throws Exception {
		Long commentId = commentService.create(me, postId, new CommentCreateRequest("댓글")).id();
		Long other = TestUsers.approved(entityManager, "김사자");

		mockMvc.perform(delete("/api/v1/posts/{postId}/comments/{commentId}", postId, commentId).header(USER_HEADER, other))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("NOT_OWNER"))
				.andExpect(jsonPath("$.message").value("본인이 작성한 댓글만 삭제할 수 있어요."));
	}

	@Test
	void 없는_댓글을_삭제하면_404() throws Exception {
		mockMvc.perform(delete("/api/v1/posts/{postId}/comments/{commentId}", postId, 999_999L).header(USER_HEADER, me))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
	}
}
