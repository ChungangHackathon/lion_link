package com.lionlink.backend.domain.profile.coffee.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** PROFILE-15 PUT. 날짜 범위 규칙은 오늘 날짜가 필요해서 서비스에서 검사한다. 겹친 기간을 합치는 옵션은 없다. */
public record CoffeePeriodUpdateRequest(
		@NotNull(message = "시작일을 입력해 주세요.")
		LocalDate startDate,

		@NotNull(message = "종료일을 입력해 주세요.")
		LocalDate endDate
) {
}
