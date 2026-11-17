package com.lionlink.backend.domain.home.dto.response;

import java.time.LocalDateTime;

public record GuideDismissResponse(boolean visible, LocalDateTime dismissedAt) {
}
