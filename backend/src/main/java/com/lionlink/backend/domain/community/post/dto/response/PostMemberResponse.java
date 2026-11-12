package com.lionlink.backend.domain.community.post.dto.response;

import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.UserSummary;
import java.util.List;

public record PostMemberResponse(
		Long id,
		String name,
		Integer generation,
		List<LionPart> lionParts,
		String photoUrl,
		boolean isHost,
		boolean isDeleted
) {

	public static PostMemberResponse of(UserSummary summary, boolean host) {
		return new PostMemberResponse(
				summary.id(),
				summary.name(),
				summary.generation(),
				summary.lionParts(),
				summary.photoUrl(),
				host,
				summary.deleted()
		);
	}
}
