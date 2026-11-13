package com.lionlink.backend.domain.coffeechat.dto.response;

import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatRejectReason;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatStatus;
import com.lionlink.backend.domain.coffeechat.entity.MeetingMode;
import com.lionlink.backend.domain.coffeechat.entity.PreferredMode;
import java.time.LocalDateTime;
import java.util.List;

public record CoffeeChatDetailResponse(
		Long id,
		CoffeeChatStatus status,
		CoffeeChatRole myRole,
		CoffeeChatUserResponse receiver,
		CoffeeChatUserResponse requester,
		List<CoffeeChatUserResponse> members,
		String topic,
		String topicCustom,
		String reason,
		List<String> questions,
		PreferredMode preferredMode,
		String scheduleNote,
		List<LocalDateTime> proposedSlots,
		String responseMessage,
		CoffeeChatRejectReason rejectReason,
		MeetingMode meetingMode,
		LocalDateTime scheduledAt,
		String meetingPlace,
		String meetingLink,
		String cancelReason,
		Long canceledBy,
		Long roomId,
		Long recruitPostId,
		LocalDateTime createdAt,
		LocalDateTime expiresAt,
		LocalDateTime acceptedAt,
		LocalDateTime rejectedAt,
		LocalDateTime canceledAt,
		LocalDateTime completedAt,
		List<CoffeeChatAction> actions
) {
}
