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
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "email_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailLog extends BaseCreatedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private String toEmail;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 30)
	private EmailType type;

	@JdbcTypeCode(SqlTypes.JSON)
	private List<Long> targetIds;

	@Column(nullable = false, length = 200)
	private String subject;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private EmailStatus status;

	@Column(nullable = false)
	private LocalDateTime scheduledAt;

	private LocalDateTime sentAt;

	@Builder
	private EmailLog(Long userId, String toEmail, EmailType type, List<Long> targetIds,
			String subject, LocalDateTime scheduledAt) {
		this.userId = userId;
		this.toEmail = toEmail;
		this.type = type;
		this.targetIds = targetIds;
		this.subject = subject;
		this.scheduledAt = scheduledAt;
		this.status = EmailStatus.PENDING;
	}
}
