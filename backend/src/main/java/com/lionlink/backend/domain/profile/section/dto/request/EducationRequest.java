package com.lionlink.backend.domain.profile.section.dto.request;

import com.lionlink.backend.domain.profile.section.education.Degree;
import com.lionlink.backend.domain.profile.validation.PeriodRequest;
import com.lionlink.backend.domain.profile.validation.ValidPeriod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.YearMonth;

/** PROFILE-11 학력 추가·수정. */
@ValidPeriod
public record EducationRequest(
		@NotBlank(message = "학교명을 입력해 주세요.")
		@Size(max = 30, message = "학교명은 30자 이하로 입력해 주세요.")
		String school,

		@Size(max = 30, message = "전공은 30자 이하로 입력해 주세요.")
		String major,

		Degree degree,

		YearMonth startYm,

		YearMonth endYm,

		Boolean isCurrent,

		@Size(max = 200, message = "설명은 200자 이하로 입력해 주세요.")
		String description
) implements PeriodRequest {
}
