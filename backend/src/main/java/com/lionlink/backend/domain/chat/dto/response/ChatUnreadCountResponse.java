package com.lionlink.backend.domain.chat.dto.response;

public record ChatUnreadCountResponse(long unreadMessages, long messageRequests) {
}
