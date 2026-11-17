package com.lionlink.backend.domain.alumni.dto.response;

public record BookmarkResponse(Long userId, boolean bookmarked) {

	public static BookmarkResponse bookmarked(Long userId) {
		return new BookmarkResponse(userId, true);
	}
}
