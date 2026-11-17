package com.lionlink.backend.domain.profile.basic.dto.request;

import jakarta.validation.constraints.Size;

/** PROFILE-06. null이나 빈 문자열을 보내면 소개가 비워진다. */
public record BioUpdateRequest(
		@Size(max = 1000, message = "소개는 1000자 이하로 입력해 주세요.") String bio
) {
}
