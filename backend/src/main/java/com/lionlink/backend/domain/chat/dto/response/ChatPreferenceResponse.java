package com.lionlink.backend.domain.chat.dto.response;

public record ChatPreferenceResponse(Long roomId, boolean pinned, boolean muted) {
}
