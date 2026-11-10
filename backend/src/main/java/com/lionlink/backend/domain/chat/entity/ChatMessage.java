package com.lionlink.backend.domain.chat.entity;

import com.lionlink.backend.global.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
		name = "chat_messages",
		uniqueConstraints = @UniqueConstraint(name = "uk_chat_messages_client_msg", columnNames = {"sender_id", "client_msg_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseCreatedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id", nullable = false)
	private ChatRoom room;

	private Long senderId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 10)
	private ChatMessageType type;

	@Column(nullable = false, length = 1000)
	private String content;

	@Column(length = 36)
	private String clientMsgId;

	private LocalDateTime editedAt;

	private LocalDateTime deletedAt;

	@Builder
	private ChatMessage(ChatRoom room, Long senderId, ChatMessageType type, String content, String clientMsgId) {
		this.room = room;
		this.senderId = senderId;
		this.type = type;
		this.content = content;
		this.clientMsgId = clientMsgId;
	}
}
