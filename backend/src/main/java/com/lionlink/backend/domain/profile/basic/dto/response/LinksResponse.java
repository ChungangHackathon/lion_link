package com.lionlink.backend.domain.profile.basic.dto.response;

import com.lionlink.backend.domain.profile.basic.dto.response.ProfileResponse.LinkItem;
import com.lionlink.backend.domain.profile.basic.entity.Profile;
import java.util.List;

/** PROFILE-08 응답. 항목 형식은 PROFILE-02의 links와 같다. 비었으면 빈 배열. */
public record LinksResponse(List<LinkItem> links) {

	public static LinksResponse from(Profile profile) {
		List<LinkItem> items = profile.getLinks() == null
				? List.of()
				: profile.getLinks().stream().map(LinkItem::from).toList();
		return new LinksResponse(items);
	}
}
