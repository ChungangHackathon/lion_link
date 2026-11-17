package com.lionlink.backend.domain.notification.event;

import com.lionlink.backend.domain.admin.event.SignupApprovedEvent;
import com.lionlink.backend.domain.chat.event.ChatNotificationEvent;
import com.lionlink.backend.domain.chat.service.ChatRequestService;
import com.lionlink.backend.domain.coffeechat.event.CoffeeChatNotificationEvent;
import com.lionlink.backend.domain.coffeechat.event.CoffeeChatNotificationType;
import com.lionlink.backend.domain.notification.entity.EmailType;
import com.lionlink.backend.domain.notification.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 다른 영역의 이벤트를 받아 메일을 쌓는다(PRD 9.2). 메일은 커피챗 신청, 메시지 요청, 가입 승인 세 가지뿐이다.
 * 요청과 같은 트랜잭션에서 쌓아서, 요청이 롤백되면 메일도 남지 않는다.
 */
@Component
@RequiredArgsConstructor
public class EmailNotificationListener {

	private final EmailNotificationService emailNotificationService;

	@EventListener
	public void onCoffeeChat(CoffeeChatNotificationEvent event) {
		if (event.type() != CoffeeChatNotificationType.COFFEE_CHAT_REQUESTED) {
			return;
		}
		event.userIds().forEach(userId ->
				emailNotificationService.enqueue(userId, EmailType.COFFEE_CHAT_REQUEST, event.coffeeChatId()));
	}

	@EventListener
	public void onChatRequested(ChatNotificationEvent event) {
		if (ChatRequestService.MESSAGE_REQUESTED.equals(event.type())) {
			emailNotificationService.enqueue(event.userId(), EmailType.MESSAGE_REQUEST, event.roomId());
		}
	}

	@EventListener
	public void onSignupApproved(SignupApprovedEvent event) {
		emailNotificationService.enqueue(event.userId(), EmailType.SIGNUP_APPROVED, event.userId());
	}
}
