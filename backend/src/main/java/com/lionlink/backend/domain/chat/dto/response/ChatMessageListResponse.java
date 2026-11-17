package com.lionlink.backend.domain.chat.dto.response;

import java.util.List;

public record ChatMessageListResponse(List<ChatMessageResponse> items, boolean hasMore) {
}
