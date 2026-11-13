package com.lionlink.backend.domain.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatSocketMessage(
		String type,
		Long roomId,
		ChatMessageResponse message,
		Long userId,
		Long lastReadMessageId,
		Boolean muted
) {

	public static ChatSocketMessage message(Long roomId, ChatMessageResponse message) {
		return new ChatSocketMessage("MESSAGE", roomId, message, null, null, null);
	}

	public static ChatSocketMessage read(Long roomId, Long userId, Long lastReadMessageId) {
		return new ChatSocketMessage("READ", roomId, null, userId, lastReadMessageId, null);
	}

	public static ChatSocketMessage roomUpdated(Long roomId, boolean muted) {
		return new ChatSocketMessage("ROOM_UPDATED", roomId, null, null, null, muted);
	}

	public static ChatSocketMessage requestReceived(Long roomId) {
		return new ChatSocketMessage("REQUEST_RECEIVED", roomId, null, null, null, null);
	}
}
