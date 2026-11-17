package com.lionlink.backend.domain.community.post.dto.response;

import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.UserSummary;
import java.util.List;

public record PostAuthorResponse(
		Long id,
		String name,
		Integer generation,
		List<LionPart> lionParts,
		String photoUrl,
		String headline,
		boolean isDeleted
) {

	public static PostAuthorResponse from(UserSummary summary) {
		return new PostAuthorResponse(
				summary.id(),
				summary.name(),
				summary.generation(),
				summary.lionParts(),
				summary.photoUrl(),
				summary.headline(),
				summary.deleted()
		);
	}
}
