package com.lionlink.backend.domain.community.post.dto.response;

import java.util.List;

public record PopularKeywordResponse(List<Item> items) {

	public record Item(String keyword, long count) {
	}
}
