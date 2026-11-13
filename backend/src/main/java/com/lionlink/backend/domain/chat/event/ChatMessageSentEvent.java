package com.lionlink.backend.domain.chat.event;

import com.lionlink.backend.domain.chat.dto.response.ChatMessageResponse;
import java.util.Map;

public record ChatMessageSentEvent(Long roomId, ChatMessageResponse message, Map<Long, Boolean> recipients) {
}
