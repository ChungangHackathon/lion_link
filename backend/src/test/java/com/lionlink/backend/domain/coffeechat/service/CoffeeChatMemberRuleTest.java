package com.lionlink.backend.domain.coffeechat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatAcceptRequest;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatCancelRequest;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatCreateRequest;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatScheduleRequest;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeSetting;
import com.lionlink.backend.domain.coffeechat.entity.MeetingMode;
import com.lionlink.backend.domain.coffeechat.entity.PreferredMode;
import com.lionlink.backend.domain.coffeechat.exception.CoffeeChatErrorCode;
import com.lionlink.backend.domain.notification.entity.Notification;
import com.lionlink.backend.domain.notification.repository.NotificationRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.ErrorCode;
import com.lionlink.backend.support.TestProfiles;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CoffeeChatMemberRuleTest {

	@Autowired
	private CoffeeChatService coffeeChatService;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private EntityManager entityManager;

	private Long me;
	private Long senior;
	private Long kim;
	private Long lee;

	@BeforeEach
	void setUp() {
		me = alumni("나사자");
		senior = alumni("선배사자");
		kim = alumni("김사자");
		lee = alumni("이사자");
		entityManager.persist(CoffeeSetting.builder().userId(senior).topics(List.of("CAREER"))
				.modes(List.of(MeetingMode.ONLINE)).build());
		entityManager.flush();
	}

	@Test
	void 함께_신청하면_동반자에게도_알림이_간다() {
		coffeeChatService.create(me, request(List.of(kim, lee)));
		flush();

		assertThat(notificationsOf("COFFEE_CHAT_JOINED"))
				.extracting(Notification::getUserId)
				.containsExactlyInAnyOrder(kim, lee);
		assertThat(notificationsOf("COFFEE_CHAT_JOINED"))
				.allSatisfy(notification -> {
					assertThat(notification.getActorId()).isEqualTo(me);
					assertThat(notification.getMessage()).isEqualTo("님이 커피챗에 함께 신청했어요.");
				});
	}

	@Test
	void 받는_사람에게는_신청_알림이_간다() {
		coffeeChatService.create(me, request(List.of(kim)));
		flush();

		assertThat(notificationsOf("COFFEE_CHAT_REQUESTED"))
				.extracting(Notification::getUserId)
				.containsExactly(senior);
	}

	@Test
	void 혼자_신청하면_동반자_알림이_없다() {
		coffeeChatService.create(me, request(List.of()));
		flush();

		assertThat(notificationsOf("COFFEE_CHAT_JOINED")).isEmpty();
	}

	@Test
	void 일정_확정은_대표_신청자와_받는_사람만_할_수_있다() {
		Long chatId = acceptedChat(List.of(kim));

		assertError(() -> coffeeChatService.schedule(kim, chatId, schedule()), CoffeeChatErrorCode.FORBIDDEN_ACTION);

		assertThat(coffeeChatService.schedule(me, chatId, schedule()).scheduledAt()).isNotNull();
		assertThat(coffeeChatService.schedule(senior, chatId, schedule()).scheduledAt()).isNotNull();
	}

	@Test
	void 팀원은_커피챗을_취소할_수_없다() {
		Long chatId = acceptedChat(List.of(kim));

		assertError(() -> coffeeChatService.cancel(kim, chatId, new CoffeeChatCancelRequest("사정이 생겼어요")),
				CoffeeChatErrorCode.FORBIDDEN_ACTION);
	}

	private Long acceptedChat(List<Long> memberIds) {
		Long chatId = coffeeChatService.create(me, request(memberIds)).id();
		coffeeChatService.accept(senior, chatId, new CoffeeChatAcceptRequest(List.of(LocalDateTime.now().plusDays(1)), null));
		return chatId;
	}

	private CoffeeChatCreateRequest request(List<Long> memberIds) {
		return new CoffeeChatCreateRequest(senior, "CAREER", null, "선배님 이야기를 듣고 싶어요.",
				null, PreferredMode.ONLINE, null, memberIds, null);
	}

	private CoffeeChatScheduleRequest schedule() {
		return new CoffeeChatScheduleRequest(LocalDateTime.now().plusDays(2), MeetingMode.ONLINE, null, "https://meet.example.com");
	}

	private List<Notification> notificationsOf(String type) {
		return notificationRepository.findAll().stream()
				.filter(notification -> type.equals(notification.getType()))
				.toList();
	}

	private Long alumni(String name) {
		Long userId = TestUsers.approved(entityManager, name);
		TestProfiles.save(entityManager, TestProfiles.profile(userId));
		return userId;
	}

	private void flush() {
		entityManager.flush();
		entityManager.clear();
	}

	private void assertError(ThrowingCallable callable, ErrorCode errorCode) {
		assertThatThrownBy(callable)
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(errorCode);
	}
}
