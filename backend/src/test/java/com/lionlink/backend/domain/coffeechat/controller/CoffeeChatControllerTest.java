package com.lionlink.backend.domain.coffeechat.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeSetting;
import com.lionlink.backend.domain.coffeechat.entity.MeetingMode;
import com.lionlink.backend.global.security.DevHeaderCurrentUserProvider;
import com.lionlink.backend.support.TestProfiles;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
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
class CoffeeChatControllerTest {

	private static final String USER_HEADER = DevHeaderCurrentUserProvider.HEADER;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EntityManager entityManager;

	private Long me;
	private Long senior;
	private Long kim;

	@BeforeEach
	void setUp() {
		me = alumni("나사자");
		senior = alumni("선배사자");
		kim = alumni("김사자");
		entityManager.persist(CoffeeSetting.builder().userId(senior).topics(List.of("CAREER"))
				.modes(List.of(MeetingMode.ONLINE)).build());
		entityManager.flush();
	}

	@Test
	void 커피챗을_신청한다() throws Exception {
		mockMvc.perform(post("/api/v1/coffee-chats")
						.header(USER_HEADER, me)
						.contentType(MediaType.APPLICATION_JSON)
						.content(createBody(List.of())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.expiresAt").value(endsWith("+09:00")));
	}

	@Test
	void 신청_입력값이_잘못되면_400() throws Exception {
		mockMvc.perform(post("/api/v1/coffee-chats")
						.header(USER_HEADER, me)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "receiverId": %d, "topic": "CAREER", "reason": "짧아요", "preferredMode": "SOMETIMES" }
								""".formatted(senior)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void 목록과_상세를_조회한다() throws Exception {
		Long id = create(List.of(kim));

		mockMvc.perform(get("/api/v1/coffee-chats").param("box", "RECEIVED").header(USER_HEADER, senior))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].id").value(id))
				.andExpect(jsonPath("$.items[0].myRole").value("RECEIVER"))
				.andExpect(jsonPath("$.items[0].counterpart.name").value("나사자"))
				.andExpect(jsonPath("$.items[0].members[0].name").value("김사자"))
				.andExpect(jsonPath("$.pendingReceivedCount").value(1))
				.andExpect(jsonPath("$.hasNext").value(false));

		mockMvc.perform(get("/api/v1/coffee-chats/{id}", id).header(USER_HEADER, senior))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.receiver.id").value(senior))
				.andExpect(jsonPath("$.requester.id").value(me))
				.andExpect(jsonPath("$.preferredMode").value("ANY"))
				.andExpect(jsonPath("$.questions[0]").value("어떻게 공부하셨나요?"))
				.andExpect(jsonPath("$.roomId").doesNotExist())
				.andExpect(jsonPath("$.actions", hasSize(2)))
				.andExpect(jsonPath("$.actions[0]").value("ACCEPT"));

		mockMvc.perform(get("/api/v1/coffee-chats/{id}", id).header(USER_HEADER, alumni("모르는사자")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("COFFEE_CHAT_NOT_FOUND"));
	}

	@Test
	void 수락하고_일정을_정한다() throws Exception {
		Long id = create(List.of());
		String slot = LocalDateTime.now().plusDays(2).withNano(0).toString();

		mockMvc.perform(post("/api/v1/coffee-chats/{id}/accept", id)
						.header(USER_HEADER, senior)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "proposedSlots": ["%s"], "responseMessage": "반가워요" }
								""".formatted(slot)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACCEPTED"))
				.andExpect(jsonPath("$.proposedSlots[0]").value(endsWith("+09:00")))
				.andExpect(jsonPath("$.roomId").isNumber());

		mockMvc.perform(put("/api/v1/coffee-chats/{id}/schedule", id)
						.header(USER_HEADER, me)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "scheduledAt": "%s", "meetingMode": "OFFLINE" }
								""".formatted(slot)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].message").value("오프라인 만남은 장소를 입력해 주세요."));

		mockMvc.perform(put("/api/v1/coffee-chats/{id}/schedule", id)
						.header(USER_HEADER, me)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "scheduledAt": "%s", "meetingMode": "OFFLINE", "meetingPlace": "강남역" }
								""".formatted(slot)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SCHEDULED"))
				.andExpect(jsonPath("$.meetingPlace").value("강남역"))
				.andExpect(jsonPath("$.actions[0]").value("SCHEDULE"));
	}

	@Test
	void 지난_시간은_후보로_고를_수_없다() throws Exception {
		Long id = create(List.of());

		mockMvc.perform(post("/api/v1/coffee-chats/{id}/accept", id)
						.header(USER_HEADER, senior)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "proposedSlots": ["%s"] }
								""".formatted(LocalDateTime.now().minusDays(1).withNano(0))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void 받는_사자가_아니면_수락할_수_없다() throws Exception {
		Long id = create(List.of());

		mockMvc.perform(post("/api/v1/coffee-chats/{id}/accept", id)
						.header(USER_HEADER, me)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "proposedSlots": ["%s"] }
								""".formatted(LocalDateTime.now().plusDays(1).withNano(0))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN_ACTION"));
	}

	@Test
	void 함께_신청자가_빠지면_204() throws Exception {
		Long id = create(List.of(kim));

		mockMvc.perform(delete("/api/v1/coffee-chats/{id}/members/me", id).header(USER_HEADER, kim))
				.andExpect(status().isNoContent());
	}

	@Test
	void 로그인하지_않으면_401() throws Exception {
		mockMvc.perform(get("/api/v1/coffee-chats"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	private Long create(List<Long> memberIds) throws Exception {
		String body = mockMvc.perform(post("/api/v1/coffee-chats")
						.header(USER_HEADER, me)
						.contentType(MediaType.APPLICATION_JSON)
						.content(createBody(memberIds)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

	private String createBody(List<Long> memberIds) {
		return """
				{ "receiverId": %d, "topic": "CAREER", "reason": "선배님의 백엔드 경험이 궁금해요.",
				  "questions": ["어떻게 공부하셨나요?"], "preferredMode": "ANY", "memberIds": %s }
				""".formatted(senior, memberIds);
	}

	private Long alumni(String name) {
		Long userId = TestUsers.approved(entityManager, name);
		TestProfiles.save(entityManager, TestProfiles.profile(userId));
		return userId;
	}
}
