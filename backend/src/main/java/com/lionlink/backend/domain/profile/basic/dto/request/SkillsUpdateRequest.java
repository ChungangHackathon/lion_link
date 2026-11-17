package com.lionlink.backend.domain.profile.basic.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** PROFILE-07. 6개 이상이면 중복을 지우기 전 개수 기준으로 400이다. null이나 빈 배열은 섹션 비우기. */
public record SkillsUpdateRequest(
		@Size(max = 5, message = "대표 기술은 최대 5개까지 입력할 수 있어요.")
		List<@NotBlank(message = "기술 이름을 입력해 주세요.")
		@Size(max = 20, message = "기술 이름은 20자 이하로 입력해 주세요.") String> skills
) {
}
