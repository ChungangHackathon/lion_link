package com.lionlink.backend.domain.community.event;

import java.util.List;

public record MeetupCanceledEvent(Long postId, String title, List<Long> memberIds) {
}
