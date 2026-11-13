package com.lionlink.backend.domain.chat.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.lionlink.backend.domain.community.post.service.PostService;
import com.lionlink.backend.global.security.DevHeaderCurrentUserProvider;
import com.lionlink.backend.support.TestPosts;
import com.lionlink.backend.support.TestProfiles;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
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
class ChatControllerTest {

	private static final String USER_HEADER = DevHeaderCurrentUserProvider.HEADER;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private PostService postService;

	private Long me;
	private Long kim;

	@BeforeEach
	void setUp() {
		me = alumni("나사자");
		kim = alumni("김사자");
	}

	@Test
	void 번개_단체방은_목록에_제목을_상세에_번개_정보를_준다() throws Exception {
		Long postId = postService.create(me, TestPosts.meetup("금요일 저녁 번개", 4, List.of(kim))).id();

		String list = mockMvc.perform(get("/api/v1/chat-rooms").header(USER_HEADER, kim))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].type").value("GROUP"))
				.andExpect(jsonPath("$.items[0].meetupId").value(postId))
				.andExpect(jsonPath("$.items[0].title").value("금요일 저녁 번개"))
				.andExpect(jsonPath("$.items[0].members[0].id").value(me))
				.andReturn().getResponse().getContentAsString();
		Long roomId = ((Number) JsonPath.read(list, "$.items[0].roomId")).longValue();

		mockMvc.perform(get("/api/v1/chat-rooms/{roomId}", roomId).header(USER_HEADER, kim))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.meetup.postId").value(postId))
				.andExpect(jsonPath("$.meetup.meetAt").value(endsWith("+09:00")))
				.andExpect(jsonPath("$.meetup.status").value("OPEN"))
				.andExpect(jsonPath("$.coffeeChat").doesNotExist())
				.andExpect(jsonPath("$.canSend").value(true));

		mockMvc.perform(delete("/api/v1/chat-rooms/{roomId}/members/me", roomId).header(USER_HEADER, kim))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INVALID_ROOM_STATUS"));
	}

	@Test
	void 안_읽은_메시지와_받은_요청_수를_준다() throws Exception {
		Long roomId = request();
		mockMvc.perform(get("/api/v1/chat-rooms/unread-count").header(USER_HEADER, kim))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.unreadMessages").value(0))
				.andExpect(jsonPath("$.messageRequests").value(1));

		mockMvc.perform(post("/api/v1/chat-requests/{roomId}/accept", roomId).header(USER_HEADER, kim))
				.andExpect(status().isOk());
		for (String content : List.of("반가워요", "잘 부탁해요")) {
			mockMvc.perform(post("/api/v1/chat-rooms/{roomId}/messages", roomId)
							.header(USER_HEADER, me)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{ \"content\": \"%s\" }".formatted(content)))
					.andExpect(status().isCreated());
		}

		mockMvc.perform(get("/api/v1/chat-rooms/unread-count").header(USER_HEADER, kim))
				.andExpect(jsonPath("$.unreadMessages").value(2))
				.andExpect(jsonPath("$.messageRequests").value(0));
		mockMvc.perform(get("/api/v1/chat-rooms/unread-count").header(USER_HEADER, me))
				.andExpect(jsonPath("$.unreadMessages").value(0));
	}

	@Test
	void 메시지_요청을_보내고_받은_요청을_확인한다() throws Exception {
		Long roomId = request();

		mockMvc.perform(post("/api/v1/chat-requests")
						.header(USER_HEADER, me)
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody(kim)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.roomId").value(roomId))
				.andExpect(jsonPath("$.created").value(false));

		mockMvc.perform(get("/api/v1/chat-requests").header(USER_HEADER, kim))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].roomId").value(roomId))
				.andExpect(jsonPath("$.items[0].requester.name").value("나사자"))
				.andExpect(jsonPath("$.items[0].firstMessage.content").value("안녕하세요! 이야기 나눠요."))
				.andExpect(jsonPath("$.items[0].requestedAt").value(endsWith("+09:00")))
				.andExpect(jsonPath("$.hasNext").value(false));

		mockMvc.perform(get("/api/v1/chat-requests/{roomId}", roomId).header(USER_HEADER, kim))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.roomId").value(roomId));
	}

	@Test
	void 요청_입력값이_잘못되면_400() throws Exception {
		mockMvc.perform(post("/api/v1/chat-requests")
						.header(USER_HEADER, me)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "receiverId": %d, "content": "짧아" }
								""".formatted(kim)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		Long roomId = request();
		mockMvc.perform(post("/api/v1/chat-requests/{roomId}/decline", roomId)
						.header(USER_HEADER, kim)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "reason": "ETC" }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].message").value("기타 사유는 5~100자로 입력해 주세요."));

		mockMvc.perform(post("/api/v1/chat-requests/{roomId}/decline", roomId)
						.header(USER_HEADER, kim)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "reason": "PREFER_COFFEE_CHAT" }
								"""))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/chat-requests")
						.header(USER_HEADER, me)
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody(kim)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("REQUEST_COOLDOWN"));
	}

	@Test
	void 수락하고_메시지를_주고받는다() throws Exception {
		Long roomId = request();

		mockMvc.perform(post("/api/v1/chat-requests/{roomId}/accept", roomId).header(USER_HEADER, kim))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.type").value("DIRECT"))
				.andExpect(jsonPath("$.members[0].id").value(me))
				.andExpect(jsonPath("$.canSend").value(true));

		String sent = mockMvc.perform(post("/api/v1/chat-rooms/{roomId}/messages", roomId)
						.header(USER_HEADER, me)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "content": "반가워요", "clientMsgId": "c-1" }
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.type").value("TEXT"))
				.andExpect(jsonPath("$.senderId").value(me))
				.andExpect(jsonPath("$.unreadCount").value(1))
				.andExpect(jsonPath("$.createdAt").value(endsWith("+09:00")))
				.andReturn().getResponse().getContentAsString();
		Long messageId = ((Number) JsonPath.read(sent, "$.id")).longValue();

		mockMvc.perform(get("/api/v1/chat-rooms").header(USER_HEADER, kim))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].partner.name").value("나사자"))
				.andExpect(jsonPath("$.items[0].lastMessage.content").value("반가워요"))
				.andExpect(jsonPath("$.items[0].unreadCount").value(1))
				.andExpect(jsonPath("$.items[0].pinned").value(false));

		mockMvc.perform(get("/api/v1/chat-rooms/{roomId}/messages", roomId).header(USER_HEADER, kim))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(3)))
				.andExpect(jsonPath("$.items[1].type").value("SYSTEM"))
				.andExpect(jsonPath("$.items[2].id").value(messageId))
				.andExpect(jsonPath("$.hasMore").value(false));

		mockMvc.perform(put("/api/v1/chat-rooms/{roomId}/read", roomId)
						.header(USER_HEADER, kim)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lastReadMessageId").value(messageId));

		mockMvc.perform(patch("/api/v1/chat-rooms/{roomId}/preferences", roomId)
						.header(USER_HEADER, kim)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "pinned": true }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pinned").value(true))
				.andExpect(jsonPath("$.muted").value(false));

		mockMvc.perform(delete("/api/v1/chat-rooms/{roomId}/members/me", roomId).header(USER_HEADER, kim))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/chat-rooms").header(USER_HEADER, kim))
				.andExpect(jsonPath("$.items", hasSize(0)));
	}

	@Test
	void 내가_없는_방은_404() throws Exception {
		Long roomId = request();

		mockMvc.perform(get("/api/v1/chat-rooms/{roomId}", roomId).header(USER_HEADER, alumni("모르는사자")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));

		mockMvc.perform(post("/api/v1/chat-rooms/{roomId}/messages", roomId)
						.header(USER_HEADER, me)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "content": "수락 전이에요" }
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INVALID_ROOM_STATUS"));
	}

	private Long request() throws Exception {
		String body = mockMvc.perform(post("/api/v1/chat-requests")
						.header(USER_HEADER, me)
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody(kim)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("REQUESTED"))
				.andExpect(jsonPath("$.requestedByMe").value(true))
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(body, "$.roomId")).longValue();
	}

	private String requestBody(Long receiverId) {
		return """
				{ "receiverId": %d, "content": "안녕하세요! 이야기 나눠요." }
				""".formatted(receiverId);
	}

	private Long alumni(String name) {
		Long userId = TestUsers.approved(entityManager, name);
		TestProfiles.save(entityManager, TestProfiles.profile(userId));
		return userId;
	}
}
