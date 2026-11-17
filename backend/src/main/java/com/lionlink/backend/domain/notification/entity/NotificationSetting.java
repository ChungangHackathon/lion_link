package com.lionlink.backend.domain.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Entity
@Table(
		name = "notification_settings",
		uniqueConstraints = @UniqueConstraint(name = "uk_notification_settings_unsubscribe_token", columnNames = "unsubscribe_token")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting {

	@Id
	private Long userId;

	private boolean emailCoffeeChat;

	private boolean emailMessageRequest;

	// ddl-auto update로 컬럼이 추가될 때 기존 회원도 켜진 상태로 시작하게 기본값을 준다.
	@ColumnDefault("true")
	private boolean emailSignupApproved;

	@Column(nullable = false, length = 36)
	private String unsubscribeToken;

	@Builder
	private NotificationSetting(Long userId) {
		this.userId = userId;
		this.emailCoffeeChat = true;
		this.emailMessageRequest = true;
		this.emailSignupApproved = true;
		this.unsubscribeToken = UUID.randomUUID().toString();
	}

	/** 값이 온 항목만 바꾼다. */
	public void update(Boolean emailCoffeeChat, Boolean emailMessageRequest, Boolean emailSignupApproved) {
		if (emailCoffeeChat != null) {
			this.emailCoffeeChat = emailCoffeeChat;
		}
		if (emailMessageRequest != null) {
			this.emailMessageRequest = emailMessageRequest;
		}
		if (emailSignupApproved != null) {
			this.emailSignupApproved = emailSignupApproved;
		}
	}

	public void unsubscribe(EmailType type) {
		switch (type) {
			case COFFEE_CHAT_REQUEST -> this.emailCoffeeChat = false;
			case MESSAGE_REQUEST -> this.emailMessageRequest = false;
			case SIGNUP_APPROVED -> this.emailSignupApproved = false;
		}
	}

	/** 메일을 보내기 전에 이 종류를 받겠다고 했는지 확인한다. */
	public boolean isEmailEnabled(EmailType type) {
		return switch (type) {
			case COFFEE_CHAT_REQUEST -> emailCoffeeChat;
			case MESSAGE_REQUEST -> emailMessageRequest;
			case SIGNUP_APPROVED -> emailSignupApproved;
		};
	}
}
