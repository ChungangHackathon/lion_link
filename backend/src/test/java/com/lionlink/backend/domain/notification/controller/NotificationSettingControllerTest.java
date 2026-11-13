package com.lionlink.backend.domain.notification.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionlink.backend.domain.notification.service.NotificationSettingService;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.global.security.DevHeaderCurrentUserProvider;
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
class NotificationSettingControllerTest {

	private static final String USER_HEADER = DevHeaderCurrentUserProvider.HEADER;
	private static final String SETTINGS = "/api/v1/me/notification-settings";
	private static final String UNSUBSCRIBE = "/api/v1/email-notifications/unsubscribe";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private NotificationSettingService notificationSettingService;

	@Autowired
	private EntityManager entityManager;

	private Long me;

	@BeforeEach
	void setUp() {
		me = TestUsers.approved(entityManager, "나사자");
	}

	@Test
	void 처음_조회하면_모두_켜져_있다() throws Exception {
		mockMvc.perform(get(SETTINGS).header(USER_HEADER, me))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.emailCoffeeChat").value(true))
				.andExpect(jsonPath("$.emailMessageRequest").value(true))
				.andExpect(jsonPath("$.emailSignupApproved").value(true))
				.andExpect(jsonPath("$.unsubscribeToken").doesNotExist());
	}

	@Test
	void 보낸_항목만_바꾼다() throws Exception {
		mockMvc.perform(patch(SETTINGS).header(USER_HEADER, me)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"emailMessageRequest\":false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.emailCoffeeChat").value(true))
				.andExpect(jsonPath("$.emailMessageRequest").value(false))
				.andExpect(jsonPath("$.emailSignupApproved").value(true));
	}

	@Test
	void 본문_형식이_틀리면_400() throws Exception {
		mockMvc.perform(patch(SETTINGS).header(USER_HEADER, me)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"emailCoffeeChat\":\"maybe\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void 승인_대기_회원도_쓸_수_있고_반려_회원은_403() throws Exception {
		Long pending = TestUsers.create(entityManager, "대기사자", UserStatus.PENDING);
		Long rejected = TestUsers.create(entityManager, "반려사자", UserStatus.REJECTED);

		mockMvc.perform(get(SETTINGS).header(USER_HEADER, pending))
				.andExpect(status().isOk());
		mockMvc.perform(patch(SETTINGS).header(USER_HEADER, pending)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"emailSignupApproved\":false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.emailSignupApproved").value(false));
		mockMvc.perform(get(SETTINGS).header(USER_HEADER, rejected))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("NOT_APPROVED"));
	}

	@Test
	void 로그인하지_않으면_401() throws Exception {
		mockMvc.perform(get(SETTINGS))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void 로그인_없이_메일_링크로_수신_거부한다() throws Exception {
		String token = notificationSettingService.getOrCreate(me).getUnsubscribeToken();

		mockMvc.perform(get(UNSUBSCRIBE).param("token", token).param("type", "COFFEE_CHAT_REQUEST"))
				.andExpect(status().isNoContent());

		mockMvc.perform(get(SETTINGS).header(USER_HEADER, me))
				.andExpect(jsonPath("$.emailCoffeeChat").value(false))
				.andExpect(jsonPath("$.emailMessageRequest").value(true));
	}

	@Test
	void 잘못된_토큰이면_400() throws Exception {
		mockMvc.perform(get(UNSUBSCRIBE).param("token", "wrong-token").param("type", "MESSAGE_REQUEST"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_UNSUBSCRIBE_TOKEN"));
	}

	@Test
	void 토큰이나_종류가_없거나_종류가_틀리면_400() throws Exception {
		String token = notificationSettingService.getOrCreate(me).getUnsubscribeToken();

		mockMvc.perform(get(UNSUBSCRIBE).param("type", "MESSAGE_REQUEST"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		mockMvc.perform(get(UNSUBSCRIBE).param("token", token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		mockMvc.perform(get(UNSUBSCRIBE).param("token", token).param("type", "NEWSLETTER"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}
}
