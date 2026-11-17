package com.lionlink.backend.domain.chat.event;

public record ChatReadEvent(Long roomId, Long userId, Long lastReadMessageId) {
}
