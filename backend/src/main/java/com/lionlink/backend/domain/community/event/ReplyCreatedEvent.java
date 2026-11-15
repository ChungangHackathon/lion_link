package com.lionlink.backend.domain.community.event;

public record ReplyCreatedEvent(Long postId, String title, Long parentAuthorId, Long commenterId) {
}
