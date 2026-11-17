package com.lionlink.backend.domain.community.event;

import java.util.List;

public record RecruitCanceledEvent(Long postId, String title, List<Long> memberIds) {
}
