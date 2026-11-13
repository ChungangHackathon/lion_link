package com.lionlink.backend.domain.chat.dto.response;

import com.lionlink.backend.domain.chat.entity.ChatRoomStatus;
import com.lionlink.backend.domain.chat.entity.ChatRoomType;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ChatRoomDetailResponse(
		Long roomId,
		ChatRoomType type,
		ChatRoomStatus status,
		boolean requestedByMe,
		List<ChatUserResponse> members,
		CoffeeChat coffeeChat,
		boolean pinned,
		boolean muted,
		boolean canSend
) {

	public record CoffeeChat(Long id, CoffeeChatStatus status, LocalDateTime scheduledAt) {
	}
}
