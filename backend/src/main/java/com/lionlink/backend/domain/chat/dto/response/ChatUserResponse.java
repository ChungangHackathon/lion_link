package com.lionlink.backend.domain.chat.dto.response;

import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.UserSummary;
import java.util.List;

public record ChatUserResponse(
		Long id,
		String name,
		Integer generation,
		List<LionPart> lionParts,
		String photoUrl,
		boolean isDeleted
) {

	public static ChatUserResponse from(UserSummary summary) {
		return new ChatUserResponse(summary.id(), summary.name(), summary.generation(), summary.lionParts(),
				summary.photoUrl(), summary.deleted());
	}
}
