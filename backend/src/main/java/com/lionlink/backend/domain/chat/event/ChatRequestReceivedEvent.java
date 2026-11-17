package com.lionlink.backend.domain.chat.event;

public record ChatRequestReceivedEvent(Long roomId, Long receiverId) {
}
