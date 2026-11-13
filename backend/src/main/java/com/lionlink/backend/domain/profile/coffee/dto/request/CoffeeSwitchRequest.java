package com.lionlink.backend.domain.profile.coffee.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * PROFILE-14 홈 스위치. restFor는 지금 받는 중(기본 ON)에서 끌 때만 쓴다.
 * CUSTOM이면 startDate·endDate가 필요하고 PROFILE-15와 같은 검증을 받는다.
 */
public record CoffeeSwitchRequest(
		@NotNull(message = "켜기 또는 끄기를 선택해 주세요.")
		Boolean on,

		RestFor restFor,

		LocalDate startDate,

		LocalDate endDate,

		Boolean confirmClearPeriods
) {

	public enum RestFor {
		WEEK_1,
		WEEK_2,
		MONTH_1,
		CUSTOM,
		UNTIL_RESUME
	}
}
