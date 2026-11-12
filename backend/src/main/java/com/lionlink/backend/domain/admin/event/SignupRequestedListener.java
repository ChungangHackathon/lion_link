package com.lionlink.backend.domain.admin.event;

import com.lionlink.backend.domain.auth.event.SignupRequestedEvent;
import com.lionlink.backend.domain.notification.entity.Notification;
import com.lionlink.backend.domain.notification.entity.NotificationTab;
import com.lionlink.backend.domain.notification.entity.NotificationTargetType;
import com.lionlink.backend.domain.notification.repository.NotificationRepository;
import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.entity.UserRole;
import com.lionlink.backend.domain.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 가입 신청·재신청이 들어오면 관리자 전원에게 '새 가입 승인 요청' 알림을 보낸다. */
@Component
@RequiredArgsConstructor
public class SignupRequestedListener {

	static final String TYPE = "SIGNUP_REQUESTED";

	private final UserRepository userRepository;
	private final NotificationRepository notificationRepository;

	// 가입 신청과 같은 트랜잭션에서 처리해 알림만 빠지거나 가입만 빠지는 일이 없게 한다.
	@EventListener
	public void onSignupRequested(SignupRequestedEvent event) {
		String message = "새 가입 승인 요청이 있어요. " + event.name() + "(" + event.generation() + "기)";
		List<User> admins = userRepository.findAllByRoleAndDeletedAtIsNull(UserRole.ADMIN);
		notificationRepository.saveAll(admins.stream()
				.map(admin -> Notification.builder()
						.userId(admin.getId())
						.tab(NotificationTab.ACCOUNT)
						.type(TYPE)
						.targetType(NotificationTargetType.USER)
						.targetId(event.userId())
						.message(message)
						.build())
				.toList());
	}
}
