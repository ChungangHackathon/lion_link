package com.lionlink.backend.domain.chat.entity;

import com.lionlink.backend.global.entity.BaseCreatedEntity;
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
		name = "chat_rooms",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_chat_rooms_direct", columnNames = {"user_a_id", "user_b_id"}),
				@UniqueConstraint(name = "uk_chat_rooms_coffee_chat", columnNames = "coffee_chat_id"),
				@UniqueConstraint(name = "uk_chat_rooms_meetup", columnNames = "meetup_id")
		}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseCreatedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 10)
	private ChatRoomType type;

	@Column(name = "user_a_id")
	private Long userAId;

	@Column(name = "user_b_id")
	private Long userBId;

	private Long coffeeChatId;

	private Long meetupId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private ChatRoomStatus status;

	private Long requestedBy;

	private LocalDateTime requestedAt;

	private LocalDateTime acceptedAt;

	private LocalDateTime declinedAt;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(length = 20)
	private ChatDeclineReason declineReason;

	@Column(length = 100)
	private String declineMessage;

	private LocalDateTime lastMessageAt;

	private LocalDateTime closedAt;

	@Builder
	private ChatRoom(ChatRoomType type, Long userAId, Long userBId, Long coffeeChatId, Long meetupId, ChatRoomStatus status,
			Long requestedBy, LocalDateTime requestedAt) {
		this.type = type;
		this.userAId = userAId;
		this.userBId = userBId;
		this.coffeeChatId = coffeeChatId;
		this.meetupId = meetupId;
		this.status = status;
		this.requestedBy = requestedBy;
		this.requestedAt = requestedAt;
	}

	public void openForCoffeeChat(Long coffeeChatId, LocalDateTime now) {
		this.coffeeChatId = coffeeChatId;
		this.status = ChatRoomStatus.ACTIVE;
		if (acceptedAt == null) {
			this.acceptedAt = now;
		}
	}

	public void touch(LocalDateTime now) {
		this.lastMessageAt = now;
	}

	public boolean isDirect() {
		return type == ChatRoomType.DIRECT;
	}

	public boolean isOpen() {
		return status == ChatRoomStatus.ACTIVE && closedAt == null;
	}

	public boolean isMeetupRoom() {
		return meetupId != null;
	}

	public void close(LocalDateTime now) {
		if (closedAt == null) {
			this.closedAt = now;
		}
	}

	public boolean isRequestedBy(Long userId) {
		return userId.equals(requestedBy);
	}

	public void request(Long requesterId, LocalDateTime now) {
		this.status = ChatRoomStatus.REQUESTED;
		this.requestedBy = requesterId;
		this.requestedAt = now;
		this.acceptedAt = null;
		this.declinedAt = null;
		this.declineReason = null;
		this.declineMessage = null;
		this.lastMessageAt = now;
	}

	public void acceptRequest(LocalDateTime now) {
		this.status = ChatRoomStatus.ACTIVE;
		this.acceptedAt = now;
	}

	public void decline(ChatDeclineReason reason, String message, LocalDateTime now) {
		this.status = ChatRoomStatus.DECLINED;
		this.declineReason = reason;
		this.declineMessage = message;
		this.declinedAt = now;
	}

	public void cancelRequest() {
		this.status = ChatRoomStatus.CANCELED;
	}
}
