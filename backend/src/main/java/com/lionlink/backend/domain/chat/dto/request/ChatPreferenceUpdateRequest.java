package com.lionlink.backend.domain.chat.dto.request;

public record ChatPreferenceUpdateRequest(Boolean pinned, Boolean muted) {
}
