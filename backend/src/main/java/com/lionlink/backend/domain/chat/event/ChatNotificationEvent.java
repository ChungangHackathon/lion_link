package com.lionlink.backend.domain.chat.event;

public record ChatNotificationEvent(Long userId, String type, Long roomId, Long actorId, String message) {
}
