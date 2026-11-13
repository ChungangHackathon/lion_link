package com.lionlink.backend.domain.profile.section.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.YearMonth;

/** PROFILE-11 수상 추가·수정. */
public record AwardRequest(
		@NotBlank(message = "수상 이름을 입력해 주세요.")
		@Size(max = 30, message = "수상 이름은 30자 이하로 입력해 주세요.")
		String name,

		@Size(max = 30, message = "주최 기관은 30자 이하로 입력해 주세요.")
		String host,

		YearMonth awardedYm,

		@Size(max = 100, message = "설명은 100자 이하로 입력해 주세요.")
		String description
) {
}
