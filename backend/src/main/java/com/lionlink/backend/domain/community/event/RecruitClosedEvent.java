package com.lionlink.backend.domain.community.event;

import java.util.List;

public record RecruitClosedEvent(Long postId, String title, Long seniorId, List<Long> memberIds) {
}
