package com.lionlink.backend.domain.community.post.dto.request;

import com.lionlink.backend.domain.community.post.entity.PostCategory;

public record PostSearchCondition(
		PostCategory category,
		String q,
		PostSort sort,
		boolean recruiting,
		PostPeriod period
) {

	public enum PostSort {
		LATEST,
		VIEWS
	}

	public enum PostPeriod {
		WEEK
	}
}
