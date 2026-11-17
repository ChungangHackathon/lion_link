package com.lionlink.backend.domain.chat.event;

import java.util.Map;

public record ChatRoomUpdatedEvent(Long roomId, Map<Long, Boolean> recipients) {
}
