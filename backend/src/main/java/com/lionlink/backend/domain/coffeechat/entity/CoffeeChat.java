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
	private PreferredMode preferredMode;

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
			List<String> questions, PreferredMode preferredMode, String scheduleNote, LocalDateTime expiresAt) {
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

	public boolean isReceiver(Long userId) {
		return receiverId.equals(userId);
	}

	public boolean isRequester(Long userId) {
		return requesterId.equals(userId);
	}

	public boolean isOpen() {
		return status == CoffeeChatStatus.PENDING || status == CoffeeChatStatus.ACCEPTED
				|| status == CoffeeChatStatus.SCHEDULED;
	}

	public boolean isExpired(LocalDateTime now) {
		return status == CoffeeChatStatus.PENDING && !expiresAt.isAfter(now);
	}

	public void accept(List<String> proposedSlots, String responseMessage, LocalDateTime now) {
		this.status = CoffeeChatStatus.ACCEPTED;
		this.proposedSlots = proposedSlots;
		this.responseMessage = responseMessage;
		this.acceptedAt = now;
	}

	public void reject(CoffeeChatRejectReason rejectReason, String responseMessage, LocalDateTime now) {
		this.status = CoffeeChatStatus.REJECTED;
		this.rejectReason = rejectReason;
		this.responseMessage = responseMessage;
		this.rejectedAt = now;
		this.openFlag = null;
	}

	public void schedule(LocalDateTime scheduledAt, MeetingMode meetingMode, String meetingPlace, String meetingLink) {
		this.status = CoffeeChatStatus.SCHEDULED;
		this.scheduledAt = scheduledAt;
		this.meetingMode = meetingMode;
		this.meetingPlace = meetingPlace;
		this.meetingLink = meetingLink;
	}

	public void cancel(Long userId, String cancelReason, LocalDateTime now) {
		this.status = CoffeeChatStatus.CANCELED;
		this.canceledBy = userId;
		this.cancelReason = cancelReason;
		this.canceledAt = now;
		this.openFlag = null;
	}

	public void complete(LocalDateTime now) {
		this.status = CoffeeChatStatus.COMPLETED;
		this.completedAt = now;
		this.openFlag = null;
	}

	public void expire() {
		this.status = CoffeeChatStatus.EXPIRED;
		this.openFlag = null;
	}
}
