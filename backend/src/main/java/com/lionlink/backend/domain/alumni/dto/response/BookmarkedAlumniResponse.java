package com.lionlink.backend.domain.alumni.dto.response;

import com.lionlink.backend.domain.alumni.entity.Bookmark;
import com.lionlink.backend.domain.profile.UserSummary;
import java.time.LocalDateTime;

public record BookmarkedAlumniResponse(
		Long userId,
		String name,
		Integer generation,
		String headline,
		String photoUrl,
		boolean isDeleted,
		LocalDateTime bookmarkedAt
) {

	public static BookmarkedAlumniResponse of(Bookmark bookmark, UserSummary summary) {
		return new BookmarkedAlumniResponse(
				bookmark.getTargetUserId(),
				summary.name(),
				summary.generation(),
				summary.headline(),
				summary.photoUrl(),
				summary.deleted(),
				bookmark.getCreatedAt()
		);
	}
}
