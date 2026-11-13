package com.lionlink.backend.domain.chat.dto.response;

import com.lionlink.backend.domain.chat.entity.ChatRoomStatus;

public record ChatRequestCreateResponse(Long roomId, ChatRoomStatus status, boolean requestedByMe, boolean created) {
}
