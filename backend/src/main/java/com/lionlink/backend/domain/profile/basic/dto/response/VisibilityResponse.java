package com.lionlink.backend.domain.profile.basic.dto.response;

import com.lionlink.backend.domain.profile.basic.entity.Profile;
import com.lionlink.backend.domain.profile.basic.entity.Visibility;

/** PROFILE-10 응답. 네 값을 모두 담는다. */
public record VisibilityResponse(Settings visibility) {

	public static VisibilityResponse from(Profile profile) {
		return new VisibilityResponse(new Settings(
				profile.getEmailVisibility(), profile.getPhoneVisibility(),
				profile.getLinksVisibility(), profile.getCompanyVisibility()));
	}

	public record Settings(Visibility email, Visibility phone, Visibility links, Visibility company) {
	}
}
