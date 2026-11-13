package com.lionlink.backend.domain.community.event;

public record RecruitJoinedEvent(Long postId, String title, Long authorId, Long memberId) {
}
