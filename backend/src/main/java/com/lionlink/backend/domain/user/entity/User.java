package com.lionlink.backend.domain.user.entity;

import com.lionlink.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
		name = "users",
		uniqueConstraints = @UniqueConstraint(name = "uk_users_google_sub", columnNames = "google_sub")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String googleSub;

	@Column(nullable = false)
	private String email;

	@Column(length = 100)
	private String googleName;

	@Column(nullable = false, length = 20)
	private String name;

	@Column(nullable = false)
	private Integer generation;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private UserRole role;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private UserStatus status;

	private Long reviewedBy;

	private LocalDateTime reviewedAt;

	@Column(length = 100)
	private String rejectReason;

	private LocalDateTime reappliedAt;

	private boolean profileCompleted;

	private LocalDateTime guideDismissedAt;

	private LocalDateTime deletedAt;

	@Builder
	private User(String googleSub, String email, String googleName, String name, Integer generation) {
		this.googleSub = googleSub;
		this.email = email;
		this.googleName = googleName;
		this.name = name;
		this.generation = generation;
		this.role = UserRole.MEMBER;
		this.status = UserStatus.PENDING;
	}

	/** AUTH-06: 승인 대기 중에만 이름·기수를 고칠 수 있다. */
	public void updateSignupInfo(String name, Integer generation) {
		if (status != UserStatus.PENDING) {
			throw new IllegalStateException("승인 대기 상태에서만 가입 정보를 고칠 수 있다. status=" + status);
		}
		this.name = name;
		this.generation = generation;
	}

	/** ADMIN-06: 관리자는 상태와 상관없이 이름·기수를 고친다. */
	public void changeSignupInfoByAdmin(String name, Integer generation) {
		this.name = name;
		this.generation = generation;
	}

	/** AUTH-07: 반려된 회원이 이름·기수를 고쳐 다시 신청한다. 이전 반려 사유는 관리자 확인용으로 남긴다. */
	public void reapply(String name, Integer generation) {
		if (status != UserStatus.REJECTED) {
			throw new IllegalStateException("반려 상태에서만 다시 신청할 수 있다. status=" + status);
		}
		this.name = name;
		this.generation = generation;
		this.status = UserStatus.PENDING;
		this.reappliedAt = LocalDateTime.now();
	}

	public void dismissGuide() {
		if (guideDismissedAt == null) {
			this.guideDismissedAt = LocalDateTime.now();
		}
	}
}
