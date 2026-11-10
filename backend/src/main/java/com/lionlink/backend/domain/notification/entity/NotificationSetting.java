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

	@Column(nullable = false, length = 36)
	private String unsubscribeToken;

	@Builder
	private NotificationSetting(Long userId) {
		this.userId = userId;
		this.emailCoffeeChat = true;
		this.emailMessageRequest = true;
		this.unsubscribeToken = UUID.randomUUID().toString();
	}
}
