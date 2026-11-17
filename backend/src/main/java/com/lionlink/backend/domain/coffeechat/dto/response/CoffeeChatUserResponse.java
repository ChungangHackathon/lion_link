package com.lionlink.backend.domain.coffeechat.dto.response;

import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.UserSummary;
import java.util.List;

public record CoffeeChatUserResponse(
		Long id,
		String name,
		Integer generation,
		List<LionPart> lionParts,
		String photoUrl,
		boolean isDeleted
) {

	public static CoffeeChatUserResponse from(UserSummary summary) {
		return new CoffeeChatUserResponse(
				summary.id(),
				summary.name(),
				summary.generation(),
				summary.lionParts(),
				summary.photoUrl(),
				summary.deleted()
		);
	}
}
