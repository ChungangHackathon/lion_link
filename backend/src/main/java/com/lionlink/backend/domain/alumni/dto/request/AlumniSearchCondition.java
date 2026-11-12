package com.lionlink.backend.domain.alumni.dto.request;

import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.ProfileStatus;
import java.util.List;

public record AlumniSearchCondition(
		String q,
		List<Integer> generations,
		List<LionPart> parts,
		List<ProfileStatus> statuses,
		List<String> interests,
		boolean available,
		AlumniSort sort
) {

	public enum AlumniSort {
		RECOMMEND,
		GENERATION,
		LATEST
	}
}
