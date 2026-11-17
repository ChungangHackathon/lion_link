package com.lionlink.backend.domain.profile;

import java.util.List;

public record UserSummary(
		Long id,
		String name,
		Integer generation,
		List<LionPart> lionParts,
		String photoUrl,
		String headline,
		boolean deleted
) {

	public static UserSummary withdrawn(Long id) {
		return new UserSummary(id, null, null, List.of(), null, null, true);
	}
}
