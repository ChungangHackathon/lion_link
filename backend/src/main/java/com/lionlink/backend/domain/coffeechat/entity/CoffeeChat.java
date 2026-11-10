package com.lionlink.backend.domain.coffeechat.entity;

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
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
		name = "coffee_chats",
		uniqueConstraints = @UniqueConstraint(name = "uk_coffee_chats_open", columnNames = {"requester_id", "receiver_id", "open_flag"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoffeeChat extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long requesterId;

	@Column(nullable = false)
	private Long receiverId;

	@Column(nullable = false, length = 30)
	private String topic;

	@Column(length = 50)
	private String topicCustom;

	@Column(nullable = false, length = 500)
	private String reason;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private List<String> questions;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 10)
	private MeetingMode preferredMode;

	@Column(length = 100)
	private String scheduleNote;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private CoffeeChatStatus status;

	// 진행 중이면 1, 끝나면 NULL. 같은 동문에게 진행 중 1건만 허용하는 UNIQUE 용도
	private Integer openFlag;

	@JdbcTypeCode(SqlTypes.JSON)
	private List<String> proposedSlots;

	@Column(length = 200)
	private String responseMessage;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(length = 30)
	private CoffeeChatRejectReason rejectReason;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(length = 10)
	private MeetingMode meetingMode;

	private LocalDateTime scheduledAt;

	@Column(length = 50)
	private String meetingPlace;

	@Column(length = 500)
	private String meetingLink;

	@Column(length = 200)
	private String cancelReason;

	private Long canceledBy;

	private LocalDateTime canceledAt;

	private LocalDateTime acceptedAt;

	private LocalDateTime rejectedAt;

	@Column(nullable = false)
	private LocalDateTime expiresAt;

	private LocalDateTime completedAt;

	@Builder
	private CoffeeChat(Long requesterId, Long receiverId, String topic, String topicCustom, String reason,
			List<String> questions, MeetingMode preferredMode, String scheduleNote, LocalDateTime expiresAt) {
		this.requesterId = requesterId;
		this.receiverId = receiverId;
		this.topic = topic;
		this.topicCustom = topicCustom;
		this.reason = reason;
		this.questions = questions;
		this.preferredMode = preferredMode;
		this.scheduleNote = scheduleNote;
		this.expiresAt = expiresAt;
		this.status = CoffeeChatStatus.PENDING;
		this.openFlag = 1;
	}
}
