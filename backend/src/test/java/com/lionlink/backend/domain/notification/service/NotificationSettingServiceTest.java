package com.lionlink.backend.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionlink.backend.domain.notification.dto.request.NotificationSettingUpdateRequest;
import com.lionlink.backend.domain.notification.dto.response.NotificationSettingResponse;
import com.lionlink.backend.domain.notification.entity.EmailType;
import com.lionlink.backend.domain.notification.entity.NotificationSetting;
import com.lionlink.backend.domain.notification.exception.NotificationErrorCode;
import com.lionlink.backend.domain.notification.repository.NotificationSettingRepository;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.CommonErrorCode;
import com.lionlink.backend.global.error.ErrorCode;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class NotificationSettingServiceTest {

	@Autowired
	private NotificationSettingService notificationSettingService;

	@Autowired
	private NotificationSettingRepository notificationSettingRepository;

	@Autowired
	private EntityManager entityManager;

	private Long me;

	@BeforeEach
	void setUp() {
		me = TestUsers.approved(entityManager, "나사자");
	}

	@Test
	void 설정이_없으면_모두_켜진_기본값으로_만든다() {
		NotificationSettingResponse response = notificationSettingService.getSettings(me);

		assertThat(response.emailCoffeeChat()).isTrue();
		assertThat(response.emailMessageRequest()).isTrue();
		assertThat(response.emailSignupApproved()).isTrue();
		NotificationSetting saved = notificationSettingRepository.findById(me).orElseThrow();
		assertThat(saved.getUnsubscribeToken()).hasSize(36);
	}

	@Test
	void 보낸_항목만_바꾼다() {
		notificationSettingService.updateSettings(me, new NotificationSettingUpdateRequest(false, null, null));
		NotificationSettingResponse response = notificationSettingService.updateSettings(me,
				new NotificationSettingUpdateRequest(null, false, null));

		assertThat(response.emailCoffeeChat()).isFalse();
		assertThat(response.emailMessageRequest()).isFalse();
		assertThat(response.emailSignupApproved()).isTrue();
		assertThat(notificationSettingService.getSettings(me)).isEqualTo(response);
	}

	@Test
	void 승인_대기_회원도_설정을_쓸_수_있다() {
		Long pending = TestUsers.create(entityManager, "대기사자", UserStatus.PENDING);

		NotificationSettingResponse response = notificationSettingService.updateSettings(pending,
				new NotificationSettingUpdateRequest(null, null, false));

		assertThat(response.emailSignupApproved()).isFalse();
	}

	@Test
	void 반려된_회원은_설정을_쓸_수_없다() {
		Long rejected = TestUsers.create(entityManager, "반려사자", UserStatus.REJECTED);

		assertError(() -> notificationSettingService.getSettings(rejected), CommonErrorCode.NOT_APPROVED);
		assertError(() -> notificationSettingService.updateSettings(rejected,
				new NotificationSettingUpdateRequest(false, null, null)), CommonErrorCode.NOT_APPROVED);
	}

	@Test
	void 수신_거부_토큰으로_해당_종류만_끈다() {
		String token = notificationSettingService.getOrCreate(me).getUnsubscribeToken();

		notificationSettingService.unsubscribe(token, EmailType.COFFEE_CHAT_REQUEST);

		NotificationSetting setting = notificationSettingRepository.findById(me).orElseThrow();
		assertThat(setting.isEmailEnabled(EmailType.COFFEE_CHAT_REQUEST)).isFalse();
		assertThat(setting.isEmailEnabled(EmailType.MESSAGE_REQUEST)).isTrue();
		assertThat(setting.isEmailEnabled(EmailType.SIGNUP_APPROVED)).isTrue();

		notificationSettingService.unsubscribe(token, EmailType.MESSAGE_REQUEST);
		notificationSettingService.unsubscribe(token, EmailType.SIGNUP_APPROVED);
		notificationSettingService.unsubscribe(token, EmailType.SIGNUP_APPROVED);

		NotificationSettingResponse response = notificationSettingService.getSettings(me);
		assertThat(response).isEqualTo(new NotificationSettingResponse(false, false, false));
	}

	@Test
	void 잘못된_토큰이면_에러() {
		notificationSettingService.getOrCreate(me);

		assertError(() -> notificationSettingService.unsubscribe("wrong-token", EmailType.COFFEE_CHAT_REQUEST),
				NotificationErrorCode.INVALID_UNSUBSCRIBE_TOKEN);
	}

	private void assertError(ThrowingCallable callable, ErrorCode errorCode) {
		assertThatThrownBy(callable)
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(errorCode);
	}
}
