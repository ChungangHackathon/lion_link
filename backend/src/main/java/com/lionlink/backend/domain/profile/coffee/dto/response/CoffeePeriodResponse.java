package com.lionlink.backend.domain.profile.coffee.dto.response;

import com.lionlink.backend.domain.coffeechat.entity.CoffeePeriod;
import java.time.LocalDate;

/** PROFILE-12 기간 목록의 원소이자 PROFILE-15 추가 응답. */
public record CoffeePeriodResponse(Long id, LocalDate startDate, LocalDate endDate) {

	public static CoffeePeriodResponse from(CoffeePeriod period) {
		return new CoffeePeriodResponse(period.getId(), period.getStartDate(), period.getEndDate());
	}
}
