package com.lionlink.backend.domain.profile.basic.dto.response;

import com.lionlink.backend.domain.profile.basic.entity.Profile;
import java.util.List;

/** PROFILE-04 응답. 요청 필드를 저장된 값 그대로 돌려준다. */
public record BasicInfoResponse(
		String headline,
		String major,
		String minor,
		String region,
		String status,
		String company,
		String jobTitle,
		List<String> lionParts,
		List<String> interests
) {

	public static BasicInfoResponse from(Profile profile) {
		return new BasicInfoResponse(
				profile.getHeadline(),
				profile.getMajor(),
				profile.getMinor(),
				profile.getRegion(),
				profile.getStatus().name(),
				profile.getCompany(),
				profile.getJobTitle(),
				profile.getLionParts().stream().map(Enum::name).toList(),
				profile.getInterests());
	}
}
