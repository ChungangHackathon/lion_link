package com.lionlink.backend.domain.community.post.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionlink.backend.domain.community.post.entity.PostCategory;
import com.lionlink.backend.domain.community.post.service.PostService;
import com.lionlink.backend.global.security.DevHeaderCurrentUserProvider;
import com.lionlink.backend.support.TestPosts;
import com.lionlink.backend.support.TestProfiles;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
class PostControllerTest {

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
	}

	@Test
	void 번개_게시글을_작성하면_201() throws Exception {
		String meetAt = LocalDateTime.now().plusDays(2).withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "+09:00";
		String body = """
				{
				  "category": "MEETUP",
				  "title": "금요일 저녁 번개",
				  "content": "같이 저녁 먹어요.",
				  "links": [],
				  "isAnonymous": false,
				  "meetup": { "meetAt": "%s", "place": "흑석역 2번 출구", "capacity": 6, "companionIds": [] }
				}
				""".formatted(meetAt);

		mockMvc.perform(post("/api/v1/posts").header(USER_HEADER, me).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.category").value("MEETUP"))
				.andExpect(jsonPath("$.createdAt").value(endsWith("+09:00")));
	}

	@Test
	void 입력값이_잘못되면_400과_fieldErrors() throws Exception {
		String body = """
				{ "category": "FREE", "title": "a", "content": "본문", "links": ["ftp://x", "https://a", "https://b", "https://c"] }
				""";

		mockMvc.perform(post("/api/v1/posts").header(USER_HEADER, me).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'title')].message").value("제목은 2~50자여야 합니다."))
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'links')]").exists());
	}

	@Test
	void 자유_게시판이_아닌_익명_글은_400() throws Exception {
		String body = """
				{ "category": "CONTEST", "title": "공모전", "content": "본문입니다.", "isAnonymous": true }
				""";

		mockMvc.perform(post("/api/v1/posts").header(USER_HEADER, me).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("ANONYMOUS_NOT_ALLOWED"));
	}

	@Test
	void 게시글_목록을_조회한다() throws Exception {
		Long postId = postService.create(me, TestPosts.general(PostCategory.CONTEST, "핀테크 공모전")).id();

		mockMvc.perform(get("/api/v1/posts").param("category", "CONTEST").param("q", "핀테크").header(USER_HEADER, me))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].id").value(postId))
				.andExpect(jsonPath("$.items[0].category").value("CONTEST"))
				.andExpect(jsonPath("$.items[0].author.name").value("나사자"))
				.andExpect(jsonPath("$.items[0].viewCount").value(0))
				.andExpect(jsonPath("$.items[0].commentCount").value(0))
				.andExpect(jsonPath("$.items[0].meetup").value(nullValue()))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.hasNext").value(false));
	}

	@Test
	void 없는_카테고리로_조회하면_400과_fieldErrors() throws Exception {
		mockMvc.perform(get("/api/v1/posts").param("category", "COFFEE_CHAT").header(USER_HEADER, me))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("category"));
	}

	@Test
	void 카테고리별_글_수를_조회한다() throws Exception {
		postService.create(me, TestPosts.general(PostCategory.FREE, "자유 글"));

		mockMvc.perform(get("/api/v1/posts/category-counts").header(USER_HEADER, me))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(1))
				.andExpect(jsonPath("$.counts.FREE").value(1))
				.andExpect(jsonPath("$.counts.RECRUIT").value(0));
	}

	@Test
	void 번개_게시글을_상세_조회한다() throws Exception {
		Long postId = postService.create(me, TestPosts.meetup("번개", 4, List.of())).id();

		mockMvc.perform(get("/api/v1/posts/{postId}", postId).header(USER_HEADER, me))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(postId))
				.andExpect(jsonPath("$.isMine").value(true))
				.andExpect(jsonPath("$.viewCount").value(1))
				.andExpect(jsonPath("$.likeCount").value(0))
				.andExpect(jsonPath("$.files", hasSize(0)))
				.andExpect(jsonPath("$.meetup.status").value("OPEN"))
				.andExpect(jsonPath("$.meetup.members[0].isHost").value(true))
				.andExpect(jsonPath("$.meetup.meetAt").value(endsWith("+09:00")))
				.andExpect(jsonPath("$.recruit").value(nullValue()))
				.andExpect(jsonPath("$.updatedAt").value(nullValue()));
	}

	@Test
	void 없는_게시글은_404() throws Exception {
		mockMvc.perform(get("/api/v1/posts/{postId}", 999_999L).header(USER_HEADER, me))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
	}

	@Test
	void 다른_사람의_글을_수정하면_403() throws Exception {
		Long postId = postService.create(me, TestPosts.general(PostCategory.FREE, "자유 글")).id();
		Long other = TestUsers.approved(entityManager, "김사자");

		mockMvc.perform(patch("/api/v1/posts/{postId}", postId).header(USER_HEADER, other)
						.contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"바뀐 제목\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("NOT_OWNER"));
	}

	@Test
	void 게시글을_수정한다() throws Exception {
		Long postId = postService.create(me, TestPosts.general(PostCategory.FREE, "자유 글")).id();

		mockMvc.perform(patch("/api/v1/posts/{postId}", postId).header(USER_HEADER, me)
						.contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"바뀐 제목\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(postId))
				.andExpect(jsonPath("$.title").value("바뀐 제목"))
				.andExpect(jsonPath("$.updatedAt").value(endsWith("+09:00")));
	}

	@Test
	void 게시글을_삭제하면_204() throws Exception {
		Long postId = postService.create(me, TestPosts.general(PostCategory.FREE, "자유 글")).id();

		mockMvc.perform(delete("/api/v1/posts/{postId}", postId).header(USER_HEADER, me))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
		mockMvc.perform(get("/api/v1/posts/{postId}", postId).header(USER_HEADER, me))
				.andExpect(status().isNotFound());
	}

	@Test
	void 좋아요와_좋아요_취소() throws Exception {
		Long postId = postService.create(me, TestPosts.general(PostCategory.FREE, "자유 글")).id();

		mockMvc.perform(put("/api/v1/posts/{postId}/like", postId).header(USER_HEADER, me))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.postId").value(postId))
				.andExpect(jsonPath("$.liked").value(true))
				.andExpect(jsonPath("$.likeCount").value(1));
		mockMvc.perform(delete("/api/v1/posts/{postId}/like", postId).header(USER_HEADER, me))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.liked").value(false))
				.andExpect(jsonPath("$.likeCount").value(0));
	}

	@Test
	void 로그인하지_않으면_401() throws Exception {
		mockMvc.perform(get("/api/v1/posts"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}
}
