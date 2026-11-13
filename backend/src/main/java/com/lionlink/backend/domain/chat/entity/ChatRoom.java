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
				@UniqueConstraint(name = "uk_chat_rooms_coffee_chat", columnNames = "coffee_chat_id")
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

	@Builder
	private ChatRoom(ChatRoomType type, Long userAId, Long userBId, Long coffeeChatId, ChatRoomStatus status,
			Long requestedBy, LocalDateTime requestedAt) {
		this.type = type;
		this.userAId = userAId;
		this.userBId = userBId;
		this.coffeeChatId = coffeeChatId;
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
}
