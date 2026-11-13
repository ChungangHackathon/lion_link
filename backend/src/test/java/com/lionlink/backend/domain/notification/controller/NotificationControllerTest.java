package com.lionlink.backend.domain.notification.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionlink.backend.domain.notification.entity.Notification;
import com.lionlink.backend.domain.notification.entity.NotificationTab;
import com.lionlink.backend.domain.notification.entity.NotificationTargetType;
import com.lionlink.backend.domain.notification.repository.NotificationRepository;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.global.security.DevHeaderCurrentUserProvider;
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
class NotificationControllerTest {

	private static final String USER_HEADER = DevHeaderCurrentUserProvider.HEADER;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private EntityManager entityManager;

	private Long me;
	private Long kim;

	@BeforeEach
	void setUp() {
		me = TestUsers.approved(entityManager, "나사자");
		kim = TestUsers.approved(entityManager, "김사자");
	}

	@Test
	void 최근_알림과_탭별_안_읽은_개수를_준다() throws Exception {
		Long id = save(me, NotificationTab.COMMUNITY, kim);
		save(me, NotificationTab.ACCOUNT, null);

		mockMvc.perform(get("/api/v1/notifications").header(USER_HEADER, me))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(2)))
				.andExpect(jsonPath("$.items[1].id").value(id))
				.andExpect(jsonPath("$.items[1].tab").value("COMMUNITY"))
				.andExpect(jsonPath("$.items[1].type").value("COMMENT_CREATED"))
				.andExpect(jsonPath("$.items[1].targetType").value("POST"))
				.andExpect(jsonPath("$.items[1].targetId").value(10))
				.andExpect(jsonPath("$.items[1].actor.id").value(kim))
				.andExpect(jsonPath("$.items[1].actor.name").value("김사자"))
				.andExpect(jsonPath("$.items[1].actor.isDeleted").value(false))
				.andExpect(jsonPath("$.items[1].message").value("님이 내 글에 댓글을 남겼어요. 자유 글"))
				.andExpect(jsonPath("$.items[1].read").value(false))
				.andExpect(jsonPath("$.items[1].createdAt").value(endsWith("+09:00")))
				.andExpect(jsonPath("$.items[0].tab").value("ACCOUNT"))
				.andExpect(jsonPath("$.items[0].actor").isEmpty())
				.andExpect(jsonPath("$.unreadCounts.all").value(2))
				.andExpect(jsonPath("$.unreadCounts.coffeeChat").value(0))
				.andExpect(jsonPath("$.unreadCounts.chat").value(0))
				.andExpect(jsonPath("$.unreadCounts.community").value(1));
	}

	@Test
	void 탭으로_거른다() throws Exception {
		save(me, NotificationTab.COMMUNITY, kim);
		Long chat = save(me, NotificationTab.CHAT, kim);

		mockMvc.perform(get("/api/v1/notifications").param("tab", "CHAT").header(USER_HEADER, me))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].id").value(chat))
				.andExpect(jsonPath("$.unreadCounts.all").value(2));
	}

	@Test
	void 지원하지_않는_탭이면_400() throws Exception {
		mockMvc.perform(get("/api/v1/notifications").param("tab", "ACCOUNT").header(USER_HEADER, me))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("tab"));
	}

	@Test
	void 알림_하나를_읽음_처리하면_204() throws Exception {
		Long id = save(me, NotificationTab.COMMUNITY, kim);

		mockMvc.perform(put("/api/v1/notifications/{notificationId}/read", id).header(USER_HEADER, me))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
		mockMvc.perform(put("/api/v1/notifications/{notificationId}/read", id).header(USER_HEADER, me))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/notifications").header(USER_HEADER, me))
				.andExpect(jsonPath("$.items[0].read").value(true))
				.andExpect(jsonPath("$.unreadCounts.all").value(0));
	}

	@Test
	void 남의_알림을_읽음_처리하면_404() throws Exception {
		Long kimsNotification = save(kim, NotificationTab.COMMUNITY, me);

		mockMvc.perform(put("/api/v1/notifications/{notificationId}/read", kimsNotification).header(USER_HEADER, me))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
	}

	@Test
	void 모두_읽음_처리하면_204() throws Exception {
		save(me, NotificationTab.COMMUNITY, kim);
		save(me, NotificationTab.COFFEE_CHAT, kim);

		mockMvc.perform(put("/api/v1/notifications/read-all").header(USER_HEADER, me))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/notifications").header(USER_HEADER, me))
				.andExpect(jsonPath("$.items[0].read").value(true))
				.andExpect(jsonPath("$.items[1].read").value(true))
				.andExpect(jsonPath("$.unreadCounts.all").value(0));
	}

	@Test
	void 로그인하지_않으면_401() throws Exception {
		mockMvc.perform(get("/api/v1/notifications"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void 승인_대기_회원은_알림을_볼_수_없다() throws Exception {
		Long pending = TestUsers.create(entityManager, "대기사자", UserStatus.PENDING);

		mockMvc.perform(get("/api/v1/notifications").header(USER_HEADER, pending))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("NOT_APPROVED"));
		mockMvc.perform(put("/api/v1/notifications/read-all").header(USER_HEADER, pending))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("NOT_APPROVED"));
	}

	private Long save(Long userId, NotificationTab tab, Long actorId) {
		Notification notification = notificationRepository.save(Notification.builder()
				.userId(userId)
				.tab(tab)
				.type(actorId == null ? "SIGNUP_APPROVED" : "COMMENT_CREATED")
				.targetType(NotificationTargetType.POST)
				.targetId(10L)
				.actorId(actorId)
				.message(actorId == null ? "가입이 승인됐어요." : "님이 내 글에 댓글을 남겼어요. 자유 글")
				.build());
		entityManager.flush();
		return notification.getId();
	}
}
