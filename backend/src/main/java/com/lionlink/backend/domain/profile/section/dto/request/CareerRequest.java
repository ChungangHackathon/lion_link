package com.lionlink.backend.domain.profile.section.dto.request;

import com.lionlink.backend.domain.profile.section.career.EmploymentType;
import com.lionlink.backend.domain.profile.validation.PeriodRequest;
import com.lionlink.backend.domain.profile.validation.ValidPeriod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.YearMonth;

/** PROFILE-11 경력 추가·수정. 온보딩이 만든 경력도 수정할 때는 시작 년월과 고용 형태를 채워야 한다. */
@ValidPeriod(requireEndOrCurrent = true)
public record CareerRequest(
		@NotBlank(message = "회사명을 입력해 주세요.")
		@Size(max = 30, message = "회사명은 30자 이하로 입력해 주세요.")
		String company,

		@NotBlank(message = "직무를 입력해 주세요.")
		@Size(max = 30, message = "직무는 30자 이하로 입력해 주세요.")
		String jobTitle,

		@NotNull(message = "고용 형태를 선택해 주세요.")
		EmploymentType employmentType,

		@NotNull(message = "시작 년월을 입력해 주세요.")
		YearMonth startYm,

		YearMonth endYm,

		Boolean isCurrent,

		@Size(max = 300, message = "설명은 300자 이하로 입력해 주세요.")
		String description
) implements PeriodRequest {
}
