package com.lionlink.backend.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.lionlink.backend.domain.admin.event.SignupApprovedEvent;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatCreateRequest;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeSetting;
import com.lionlink.backend.domain.coffeechat.entity.MeetingMode;
import com.lionlink.backend.domain.coffeechat.entity.PreferredMode;
import com.lionlink.backend.domain.coffeechat.event.CoffeeChatNotificationEvent;
import com.lionlink.backend.domain.coffeechat.event.CoffeeChatNotificationType;
import com.lionlink.backend.domain.coffeechat.service.CoffeeChatService;
import com.lionlink.backend.domain.notification.dto.request.NotificationSettingUpdateRequest;
import com.lionlink.backend.domain.notification.email.EmailSender;
import com.lionlink.backend.domain.notification.entity.EmailLog;
import com.lionlink.backend.domain.notification.entity.EmailStatus;
import com.lionlink.backend.domain.notification.entity.EmailType;
import com.lionlink.backend.domain.notification.repository.EmailLogRepository;
import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.support.TestProfiles;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class EmailNotificationServiceTest {

	@MockitoBean
	private EmailSender emailSender;

	@Autowired
	private EmailNotificationService emailNotificationService;

	@Autowired
	private NotificationSettingService notificationSettingService;

	@Autowired
	private CoffeeChatService coffeeChatService;

	@Autowired
	private EmailLogRepository emailLogRepository;

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	@Autowired
	private EntityManager entityManager;

	private Long me;
	private Long senior;

	@BeforeEach
	void setUp() {
		me = alumni("나사자");
		senior = alumni("선배사자");
		entityManager.persist(CoffeeSetting.builder().userId(senior).topics(List.of("CAREER"))
				.modes(List.of(MeetingMode.ONLINE)).build());
		entityManager.flush();
		entityManager.clear();
	}

	@Test
	void 커피챗을_신청하면_받는_사자에게_메일이_쌓이고_스케줄러가_보낸다() {
		Long coffeeChatId = requestCoffeeChat();

		EmailLog queued = onlyLog();
		assertThat(queued.getUserId()).isEqualTo(senior);
		assertThat(queued.getToEmail()).isEqualTo(email(senior));
		assertThat(queued.getType()).isEqualTo(EmailType.COFFEE_CHAT_REQUEST);
		assertThat(queued.getTargetIds()).containsExactly(coffeeChatId);
		assertThat(queued.getSubject()).isEqualTo("[LION:LINK] 나사자(13기)님이 커피챗을 신청했어요");
		assertThat(queued.getStatus()).isEqualTo(EmailStatus.PENDING);
		verify(emailSender, never()).send(anyString(), anyString(), anyString());

		assertThat(emailNotificationService.sendDue(later())).isEqualTo(1);

		ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
		verify(emailSender).send(eq(email(senior)), eq("[LION:LINK] 나사자(13기)님이 커피챗을 신청했어요"), html.capture());
		String token = notificationSettingService.getOrCreate(senior).getUnsubscribeToken();
		assertThat(html.getValue())
				.contains("커리어/이직")
				.contains("http://localhost:3000/coffee/" + coffeeChatId)
				.contains("http://localhost:3000/email-notifications/unsubscribe?token=" + token + "&amp;type=COFFEE_CHAT_REQUEST");
		EmailLog sent = onlyLog();
		assertThat(sent.getStatus()).isEqualTo(EmailStatus.SENT);
		assertThat(sent.getSentAt()).isNotNull();
		assertThat(sent.getAttemptCount()).isEqualTo(1);
	}

	@Test
	void 신청이_아닌_커피챗_알림은_메일을_보내지_않는다() {
		eventPublisher.publishEvent(new CoffeeChatNotificationEvent(List.of(senior),
				CoffeeChatNotificationType.COFFEE_CHAT_ACCEPTED, 1L, "수락했어요."));

		assertThat(logs()).isEmpty();
	}

	@Test
	void 수신을_끈_회원에게는_보내지_않는다() {
		notificationSettingService.updateSettings(senior, new NotificationSettingUpdateRequest(false, null, null));
		requestCoffeeChat();

		emailNotificationService.sendDue(later());

		assertThat(onlyLog().getStatus()).isEqualTo(EmailStatus.SKIPPED);
		verify(emailSender, never()).send(anyString(), anyString(), anyString());
	}

	@Test
	void 보내기_전에_신청이_처리됐으면_보내지_않는다() {
		Long coffeeChatId = requestCoffeeChat();
		entityManager.createNativeQuery("update coffee_chats set status = 'CANCELED', open_flag = null where id = :id")
				.setParameter("id", coffeeChatId)
				.executeUpdate();
		entityManager.clear();

		emailNotificationService.sendDue(later());

		assertThat(onlyLog().getStatus()).isEqualTo(EmailStatus.SKIPPED);
		verify(emailSender, never()).send(anyString(), anyString(), anyString());
	}

	@Test
	void 보내기_전에_탈퇴했으면_보내지_않는다() {
		requestCoffeeChat();
		TestUsers.withdraw(entityManager, senior);

		emailNotificationService.sendDue(later());

		assertThat(onlyLog().getStatus()).isEqualTo(EmailStatus.SKIPPED);
		verify(emailSender, never()).send(anyString(), anyString(), anyString());
	}

	@Test
	void 실패하면_10분_뒤에_다시_보내고_3번_실패하면_멈춘다() {
		doThrow(new MailSendException("smtp down")).when(emailSender).send(anyString(), anyString(), anyString());
		requestCoffeeChat();
		// 리눅스(CI)처럼 시계가 나노초까지 주는 경우도 같은 시각에 다시 보내는지 확인한다.
		LocalDateTime now = later().withNano(123_456_789);

		emailNotificationService.sendDue(now);
		EmailLog first = onlyLog();
		assertThat(first.getStatus()).isEqualTo(EmailStatus.PENDING);
		assertThat(first.getAttemptCount()).isEqualTo(1);
		assertThat(first.getScheduledAt()).isEqualTo(now.plusMinutes(10).truncatedTo(ChronoUnit.MICROS));

		assertThat(emailNotificationService.sendDue(now.plusMinutes(5))).isZero();
		emailNotificationService.sendDue(now.plusMinutes(10));
		emailNotificationService.sendDue(now.plusMinutes(20));

		EmailLog failed = onlyLog();
		assertThat(failed.getStatus()).isEqualTo(EmailStatus.FAILED);
		assertThat(failed.getAttemptCount()).isEqualTo(3);
		assertThat(emailNotificationService.sendDue(now.plusDays(1))).isZero();
	}

	@Test
	void 가입이_승인되면_승인_메일을_보낸다() {
		Long pending = TestUsers.create(entityManager, "새사자", UserStatus.PENDING);

		eventPublisher.publishEvent(new SignupApprovedEvent(pending, "새사자"));
		emailNotificationService.sendDue(later());

		ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
		verify(emailSender).send(eq(email(pending)), eq("[LION:LINK] 가입이 승인됐어요"), html.capture());
		assertThat(html.getValue()).contains("새사자").contains("type=SIGNUP_APPROVED");
		assertThat(onlyLog().getType()).isEqualTo(EmailType.SIGNUP_APPROVED);
	}

	@Test
	void 가입_승인_메일을_끈_회원에게는_보내지_않는다() {
		Long pending = TestUsers.create(entityManager, "새사자", UserStatus.PENDING);
		notificationSettingService.updateSettings(pending, new NotificationSettingUpdateRequest(null, null, false));

		eventPublisher.publishEvent(new SignupApprovedEvent(pending, "새사자"));
		emailNotificationService.sendDue(later());

		assertThat(onlyLog().getStatus()).isEqualTo(EmailStatus.SKIPPED);
		verify(emailSender, never()).send(anyString(), anyString(), anyString());
	}

	@Test
	void 메시지_요청_메일은_채팅에서_바로_쌓을_수_있다() {
		emailNotificationService.enqueue(senior, EmailType.MESSAGE_REQUEST, 7L);
		emailNotificationService.sendDue(later());

		verify(emailSender).send(eq(email(senior)), eq("[LION:LINK] 새 메시지 요청이 왔어요"), anyString());
		assertThat(onlyLog().getTargetIds()).containsExactly(7L);
	}

	@Test
	void 탈퇴한_회원에게는_메일을_쌓지_않는다() {
		TestUsers.withdraw(entityManager, senior);

		emailNotificationService.enqueue(senior, EmailType.MESSAGE_REQUEST, 7L);

		assertThat(logs()).isEmpty();
	}

	private Long requestCoffeeChat() {
		Long id = coffeeChatService.create(me, new CoffeeChatCreateRequest(senior, "CAREER", null,
				"선배님의 백엔드 경험이 궁금해요.", List.of(), PreferredMode.ANY, null, List.of(), null)).id();
		entityManager.flush();
		return id;
	}

	private Long alumni(String name) {
		Long userId = TestUsers.approved(entityManager, name);
		TestProfiles.save(entityManager, TestProfiles.profile(userId));
		return userId;
	}

	private String email(Long userId) {
		return entityManager.find(User.class, userId).getEmail();
	}

	private LocalDateTime later() {
		return LocalDateTime.now().plusSeconds(1);
	}

	private List<EmailLog> logs() {
		entityManager.flush();
		return emailLogRepository.findAll();
	}

	private EmailLog onlyLog() {
		List<EmailLog> logs = logs();
		assertThat(logs).hasSize(1);
		return logs.get(0);
	}
}
