package com.lionlink.backend.domain.admin.event;

import com.lionlink.backend.domain.notification.entity.Notification;
import com.lionlink.backend.domain.notification.entity.NotificationTab;
import com.lionlink.backend.domain.notification.entity.NotificationTargetType;
import com.lionlink.backend.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 가입이 승인되면 본인에게 서비스 안 알림(계정 탭)을 만든다. 승인 메일은 메일 인프라가 생기면 여기서 이어서 처리한다. */
@Component
@RequiredArgsConstructor
public class SignupApprovedListener {

	static final String TYPE = "SIGNUP_APPROVED";
	static final String MESSAGE = "가입이 승인됐어요. 이제 모든 기능을 이용할 수 있어요.";

	private final NotificationRepository notificationRepository;

	// 승인과 같은 트랜잭션에서 처리해 승인만 되고 알림이 빠지는 일이 없게 한다.
	@EventListener
	public void onSignupApproved(SignupApprovedEvent event) {
		notificationRepository.save(Notification.builder()
				.userId(event.userId())
				.tab(NotificationTab.ACCOUNT)
				.type(TYPE)
				.targetType(NotificationTargetType.USER)
				.targetId(event.userId())
				.message(MESSAGE)
				.build());
	}
}
