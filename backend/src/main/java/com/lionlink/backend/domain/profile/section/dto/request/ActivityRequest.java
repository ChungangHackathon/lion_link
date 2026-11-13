package com.lionlink.backend.domain.profile.section.dto.request;

import com.lionlink.backend.domain.profile.validation.PeriodRequest;
import com.lionlink.backend.domain.profile.validation.ValidPeriod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.YearMonth;

/** PROFILE-11 대외활동 추가·수정. 진행 중(isCurrent) 표시는 없다. */
@ValidPeriod
public record ActivityRequest(
		@NotBlank(message = "활동 이름을 입력해 주세요.")
		@Size(max = 30, message = "활동 이름은 30자 이하로 입력해 주세요.")
		String name,

		@Size(max = 30, message = "기관은 30자 이하로 입력해 주세요.")
		String organization,

		YearMonth startYm,

		YearMonth endYm,

		@Size(max = 200, message = "설명은 200자 이하로 입력해 주세요.")
		String description
) implements PeriodRequest {
}
