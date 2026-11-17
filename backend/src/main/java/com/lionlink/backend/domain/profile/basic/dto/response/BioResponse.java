package com.lionlink.backend.domain.profile.basic.dto.response;

import com.lionlink.backend.domain.profile.basic.entity.Profile;

/** PROFILE-06 응답. */
public record BioResponse(String bio) {

	public static BioResponse from(Profile profile) {
		return new BioResponse(profile.getBio());
	}
}
