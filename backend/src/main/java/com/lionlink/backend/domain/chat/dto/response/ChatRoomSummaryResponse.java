package com.lionlink.backend.domain.chat.dto.response;

import com.lionlink.backend.domain.chat.entity.ChatRoomStatus;
import com.lionlink.backend.domain.chat.entity.ChatRoomType;
import java.util.List;

public record ChatRoomSummaryResponse(
		Long roomId,
		ChatRoomType type,
		ChatRoomStatus status,
		boolean requestedByMe,
		ChatUserResponse partner,
		List<ChatUserResponse> members,
		int memberCount,
		Long coffeeChatId,
		Long meetupId,
		String title,
		ChatMessageResponse lastMessage,
		long unreadCount,
		boolean pinned,
		boolean muted
) {
}
