package com.lionlink.backend.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionlink.backend.domain.notification.dto.request.NotificationTabFilter;
import com.lionlink.backend.domain.notification.dto.response.NotificationListResponse;
import com.lionlink.backend.domain.notification.dto.response.NotificationResponse;
import com.lionlink.backend.domain.notification.entity.Notification;
import com.lionlink.backend.domain.notification.entity.NotificationTab;
import com.lionlink.backend.domain.notification.entity.NotificationTargetType;
import com.lionlink.backend.domain.notification.exception.NotificationErrorCode;
import com.lionlink.backend.domain.notification.repository.NotificationRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class NotificationServiceTest {

	@Autowired
	private NotificationService notificationService;

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
	void 전체_탭은_내_알림만_최신순으로_20개까지_준다() {
		IntStream.range(0, 21).forEach(i -> save(me, NotificationTab.COMMUNITY, kim));
		Long latest = save(me, NotificationTab.ACCOUNT, null);
		save(kim, NotificationTab.COMMUNITY, me);

		NotificationListResponse response = notificationService.getNotifications(me, NotificationTabFilter.ALL);

		assertThat(response.items()).hasSize(20);
		assertThat(response.items().get(0).id()).isEqualTo(latest);
		assertThat(response.items()).extracting(NotificationResponse::id).isSortedAccordingTo((a, b) -> Long.compare(b, a));
	}

	@Test
	void 탭을_고르면_그_탭_알림만_주고_계정_알림은_전체에서만_보인다() {
		Long coffeeChat = save(me, NotificationTab.COFFEE_CHAT, kim);
		Long chat = save(me, NotificationTab.CHAT, kim);
		Long community = save(me, NotificationTab.COMMUNITY, kim);
		Long account = save(me, NotificationTab.ACCOUNT, null);

		assertThat(ids(NotificationTabFilter.COFFEE_CHAT)).containsExactly(coffeeChat);
		assertThat(ids(NotificationTabFilter.CHAT)).containsExactly(chat);
		assertThat(ids(NotificationTabFilter.COMMUNITY)).containsExactly(community);
		assertThat(ids(NotificationTabFilter.ALL)).containsExactly(account, community, chat, coffeeChat);
	}

	@Test
	void 탭별_안_읽은_개수는_탭과_상관없이_모두_센다() {
		save(me, NotificationTab.COFFEE_CHAT, kim);
		save(me, NotificationTab.CHAT, kim);
		save(me, NotificationTab.CHAT, kim);
		save(me, NotificationTab.COMMUNITY, kim);
		save(me, NotificationTab.ACCOUNT, null);
		notificationService.read(me, save(me, NotificationTab.COMMUNITY, kim));
		save(kim, NotificationTab.COMMUNITY, me);

		NotificationListResponse response = notificationService.getNotifications(me, NotificationTabFilter.CHAT);

		assertThat(response.items()).hasSize(2);
		assertThat(response.unreadCounts().all()).isEqualTo(5);
		assertThat(response.unreadCounts().coffeeChat()).isEqualTo(1);
		assertThat(response.unreadCounts().chat()).isEqualTo(2);
		assertThat(response.unreadCounts().community()).isEqualTo(1);
	}

	@Test
	void 보낸_사람은_지금_이름으로_채우고_탈퇴했으면_표시한다() {
		Long lee = TestUsers.approved(entityManager, "이사자");
		Long fromKim = save(me, NotificationTab.COMMUNITY, kim);
		Long fromLee = save(me, NotificationTab.COMMUNITY, lee);
		Long system = save(me, NotificationTab.ACCOUNT, null);
		TestUsers.withdraw(entityManager, lee);

		NotificationListResponse response = notificationService.getNotifications(me, NotificationTabFilter.ALL);

		NotificationResponse kimItem = item(response, fromKim);
		assertThat(kimItem.actor().id()).isEqualTo(kim);
		assertThat(kimItem.actor().name()).isEqualTo("김사자");
		assertThat(kimItem.actor().isDeleted()).isFalse();
		assertThat(kimItem.message()).isEqualTo("님이 내 글에 댓글을 남겼어요. 자유 글");
		assertThat(kimItem.tab()).isEqualTo(NotificationTab.COMMUNITY);
		assertThat(kimItem.type()).isEqualTo("COMMENT_CREATED");
		assertThat(kimItem.targetType()).isEqualTo(NotificationTargetType.POST);
		assertThat(kimItem.targetId()).isEqualTo(10L);
		assertThat(kimItem.read()).isFalse();
		assertThat(kimItem.createdAt()).isNotNull();

		NotificationResponse leeItem = item(response, fromLee);
		assertThat(leeItem.actor().id()).isEqualTo(lee);
		assertThat(leeItem.actor().name()).isNull();
		assertThat(leeItem.actor().isDeleted()).isTrue();

		assertThat(item(response, system).actor()).isNull();
	}

	@Test
	void 알림을_읽으면_읽은_시각이_남고_다시_읽어도_처음_시각을_유지한다() {
		Long id = save(me, NotificationTab.COMMUNITY, kim);

		notificationService.read(me, id);
		LocalDateTime firstReadAt = notificationRepository.findById(id).orElseThrow().getReadAt();
		notificationService.read(me, id);

		assertThat(firstReadAt).isNotNull();
		assertThat(notificationRepository.findById(id).orElseThrow().getReadAt()).isEqualTo(firstReadAt);
	}

	@Test
	void 남의_알림이나_없는_알림을_읽으면_404() {
		Long kimsNotification = save(kim, NotificationTab.COMMUNITY, me);

		assertThatThrownBy(() -> notificationService.read(me, kimsNotification))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
		assertThatThrownBy(() -> notificationService.read(me, 999_999L))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
		assertThat(notificationRepository.findById(kimsNotification).orElseThrow().isRead()).isFalse();
	}

	@Test
	void 모두_읽음은_내_알림만_읽음으로_바꾼다() {
		save(me, NotificationTab.COFFEE_CHAT, kim);
		save(me, NotificationTab.ACCOUNT, null);
		Long kimsNotification = save(kim, NotificationTab.COMMUNITY, me);

		notificationService.readAll(me);

		assertThat(notificationService.getNotifications(me, NotificationTabFilter.ALL).items())
				.allMatch(NotificationResponse::read);
		assertThat(notificationService.getNotifications(me, NotificationTabFilter.ALL).unreadCounts().all()).isZero();
		assertThat(notificationRepository.findById(kimsNotification).orElseThrow().isRead()).isFalse();
	}

	private Long save(Long userId, NotificationTab tab, Long actorId) {
		Notification notification = notificationRepository.save(Notification.builder()
				.userId(userId)
				.tab(tab)
				.type("COMMENT_CREATED")
				.targetType(NotificationTargetType.POST)
				.targetId(10L)
				.actorId(actorId)
				.message(actorId == null ? "가입이 승인됐어요." : "님이 내 글에 댓글을 남겼어요. 자유 글")
				.build());
		entityManager.flush();
		return notification.getId();
	}

	private List<Long> ids(NotificationTabFilter filter) {
		return notificationService.getNotifications(me, filter).items().stream()
				.map(NotificationResponse::id)
				.toList();
	}

	private NotificationResponse item(NotificationListResponse response, Long id) {
		return response.items().stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow();
	}
}
