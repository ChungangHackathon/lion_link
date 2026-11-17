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

	/** 알림을 일으킨 회원. 화면은 이 회원 이름을 굵게 쓰고 message를 이어 붙인다. 시스템 알림이면 null */
	private Long actorId;

	/** actorId가 있으면 이름 뒤에 붙는 문구('님이 ...'), 없으면 문장 전체 */
	@Column(nullable = false, length = 300)
	private String message;

	private LocalDateTime readAt;

	@Builder
	private Notification(Long userId, NotificationTab tab, String type,
			NotificationTargetType targetType, Long targetId, Long actorId, String message) {
		this.userId = userId;
		this.tab = tab;
		this.type = type;
		this.targetType = targetType;
		this.targetId = targetId;
		this.actorId = actorId;
		this.message = message;
	}

	public boolean isRead() {
		return readAt != null;
	}

	/** 이미 읽은 알림은 처음 읽은 시각을 그대로 둔다. */
	public void read(LocalDateTime now) {
		if (readAt == null) {
			this.readAt = now;
		}
	}
}
