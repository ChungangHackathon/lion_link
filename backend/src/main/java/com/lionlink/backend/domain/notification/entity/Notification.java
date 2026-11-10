package com.lionlink.backend.domain.notification.entity;

import com.lionlink.backend.global.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseCreatedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private NotificationTab tab;

	@Column(nullable = false, length = 40)
	private String type;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(length = 20)
	private NotificationTargetType targetType;

	private Long targetId;

	@Column(nullable = false, length = 300)
	private String message;

	private LocalDateTime readAt;

	@Builder
	private Notification(Long userId, NotificationTab tab, String type,
			NotificationTargetType targetType, Long targetId, String message) {
		this.userId = userId;
		this.tab = tab;
		this.type = type;
		this.targetType = targetType;
		this.targetId = targetId;
		this.message = message;
	}
}
