package com.lionlink.backend.domain.alumni.dto.response;

import com.lionlink.backend.domain.alumni.repository.AlumniSearchRepository;
import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.ProfileStatus;
import com.lionlink.backend.domain.profile.basic.entity.Profile;
import java.util.List;
import java.util.Objects;

public record AlumniResponse(
		Long id,
		String name,
		Integer generation,
		List<LionPart> lionParts,
		ProfileStatus status,
		String headline,
		String photoUrl,
		String company,
		List<String> interests,
		List<String> skills,
		boolean bookmarked
) {

	public static AlumniResponse of(AlumniSearchRepository.Row row, String company, boolean bookmarked) {
		Profile profile = row.profile();
		return new AlumniResponse(
				row.id(),
				row.name(),
				row.generation(),
				profile.getLionParts(),
				profile.getStatus(),
				profile.getHeadline(),
				profile.getPhotoUrl(),
				company,
				profile.getInterests(),
				Objects.requireNonNullElse(profile.getSkills(), List.of()),
				bookmarked
		);
	}
}
