package com.lionlink.backend.domain.profile.basic.dto.response;

import com.lionlink.backend.domain.profile.basic.entity.Profile;
import java.util.List;

/** PROFILE-07 응답. 비었으면 빈 배열. */
public record SkillsResponse(List<String> skills) {

	public static SkillsResponse from(Profile profile) {
		return new SkillsResponse(profile.getSkills() == null ? List.of() : profile.getSkills());
	}
}
