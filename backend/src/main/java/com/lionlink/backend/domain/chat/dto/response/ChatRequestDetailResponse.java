package com.lionlink.backend.domain.chat.dto.response;

import com.lionlink.backend.domain.chat.entity.ChatDeclineReason;
import com.lionlink.backend.domain.chat.entity.ChatRoomStatus;
import java.time.LocalDateTime;

public record ChatRequestDetailResponse(
		Long roomId,
		ChatRoomStatus status,
		ChatUserResponse requester,
		ChatUserResponse receiver,
		ChatMessageResponse firstMessage,
		LocalDateTime requestedAt,
		LocalDateTime declinedAt,
		ChatDeclineReason declineReason,
		String declineMessage
) {
}
