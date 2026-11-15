package com.lionlink.backend.domain.chat.dto.response;

import java.util.List;

public record ChatRoomInviteResponse(int invitedCount, List<Long> skippedUserIds) {
}
