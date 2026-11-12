package com.lionlink.backend.domain.community.post.dto.response;

import com.lionlink.backend.domain.community.post.entity.Post;
import com.lionlink.backend.domain.community.post.entity.PostCategory;
import java.time.LocalDateTime;

public record PostCreateResponse(Long id, PostCategory category, LocalDateTime createdAt) {

	public static PostCreateResponse from(Post post) {
		return new PostCreateResponse(post.getId(), post.getCategory(), post.getCreatedAt());
	}
}
