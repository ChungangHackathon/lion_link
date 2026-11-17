package com.lionlink.backend.domain.community.event;

public record CommentCreatedEvent(Long postId, String title, Long postAuthorId, Long commenterId) {
}
