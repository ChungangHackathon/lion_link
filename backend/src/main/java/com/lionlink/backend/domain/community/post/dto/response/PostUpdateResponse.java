package com.lionlink.backend.domain.community.post.dto.response;

import com.lionlink.backend.domain.community.post.entity.Post;
import java.time.LocalDateTime;

public record PostUpdateResponse(Long id, String title, LocalDateTime updatedAt) {

	public static PostUpdateResponse from(Post post) {
		return new PostUpdateResponse(post.getId(), post.getTitle(), post.getUpdatedAt());
	}
}
