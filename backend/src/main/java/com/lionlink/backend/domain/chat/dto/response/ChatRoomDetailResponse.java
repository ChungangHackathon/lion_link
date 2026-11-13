package com.lionlink.backend.domain.chat.dto.response;

import com.lionlink.backend.domain.chat.entity.ChatRoomStatus;
import com.lionlink.backend.domain.chat.entity.ChatRoomType;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatStatus;
import com.lionlink.backend.domain.community.meetup.entity.MeetupStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ChatRoomDetailResponse(
		Long roomId,
		ChatRoomType type,
		ChatRoomStatus status,
		boolean requestedByMe,
		List<ChatUserResponse> members,
		CoffeeChat coffeeChat,
		Meetup meetup,
		boolean pinned,
		boolean muted,
		boolean canSend
) {

	public record CoffeeChat(Long id, CoffeeChatStatus status, LocalDateTime scheduledAt) {
	}

	public record Meetup(Long postId, String title, LocalDateTime meetAt, String place, MeetupStatus status) {
	}
}
