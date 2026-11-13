package com.lionlink.backend.domain.profile.basic.dto.response;

import com.lionlink.backend.domain.profile.basic.entity.Profile;

/** PROFILE-09 응답. */
public record ContactResponse(String email, String phone) {

	public static ContactResponse from(Profile profile) {
		return new ContactResponse(profile.getContactEmail(), profile.getPhone());
	}
}
