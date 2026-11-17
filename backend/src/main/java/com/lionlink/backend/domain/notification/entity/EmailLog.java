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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
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

	// ddl-auto update로 컬럼이 추가될 때 기존 행도 0에서 시작하게 기본값을 준다.
	@ColumnDefault("0")
	private int attemptCount;

	@Builder
	private EmailLog(Long userId, String toEmail, EmailType type, List<Long> targetIds,
			String subject, LocalDateTime scheduledAt) {
		this.userId = userId;
		this.toEmail = toEmail;
		this.type = type;
		this.targetIds = targetIds;
		this.subject = subject;
		this.scheduledAt = toDbPrecision(scheduledAt);
		this.status = EmailStatus.PENDING;
	}

	public Long targetId() {
		return targetIds == null || targetIds.isEmpty() ? null : targetIds.get(0);
	}

	public void markSent(LocalDateTime now) {
		this.status = EmailStatus.SENT;
		this.sentAt = now;
		this.attemptCount++;
	}

	/** 수신 거부, 탈퇴, 이미 처리된 대상처럼 보낼 필요가 없어진 메일 */
	public void skip() {
		this.status = EmailStatus.SKIPPED;
	}

	/** 실패하면 retryDelay 뒤에 다시 보낸다. maxAttempts번 실패하면 더 보내지 않는다. */
	public void fail(LocalDateTime now, int maxAttempts, Duration retryDelay) {
		this.attemptCount++;
		if (attemptCount >= maxAttempts) {
			this.status = EmailStatus.FAILED;
			return;
		}
		this.scheduledAt = toDbPrecision(now.plus(retryDelay));
	}

	// DB는 마이크로초까지만 저장하면서 반올림한다. 리눅스처럼 나노초까지 나오는 시계에서는 저장된 값이 원래보다 커져
	// '보낼 시각이 됐는지' 비교가 어긋나므로, 미리 마이크로초로 잘라 둔다.
	private static LocalDateTime toDbPrecision(LocalDateTime time) {
		return time.truncatedTo(ChronoUnit.MICROS);
	}
}
