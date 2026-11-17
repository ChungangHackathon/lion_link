package com.lionlink.backend.domain.community.post.dto.request;

import com.lionlink.backend.domain.community.post.entity.PostCategory;
import java.util.List;

public record PostSearchCondition(
		List<PostCategory> categories,
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
