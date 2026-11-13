package com.lionlink.backend.domain.chat.dto.response;

import com.lionlink.backend.domain.chat.entity.ChatMessage;
import com.lionlink.backend.domain.chat.entity.ChatMessageType;
import java.time.LocalDateTime;

public record ChatMessageResponse(
		Long id,
		ChatMessageType type,
		Long senderId,
		String content,
		boolean deleted,
		LocalDateTime createdAt,
		LocalDateTime editedAt,
		long unreadCount
) {

	public static ChatMessageResponse of(ChatMessage message, long unreadCount) {
		return new ChatMessageResponse(
				message.getId(),
				message.getType(),
				message.getSenderId(),
				message.isDeleted() ? null : message.getContent(),
				message.isDeleted(),
				message.getCreatedAt(),
				message.getEditedAt(),
				unreadCount
		);
	}
}
