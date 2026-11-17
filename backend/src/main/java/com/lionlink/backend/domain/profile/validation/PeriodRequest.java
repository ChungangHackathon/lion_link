package com.lionlink.backend.domain.profile.validation;

import java.time.YearMonth;

/** 시작·종료 년월을 가진 요청. {@link ValidPeriod}가 검사한다. isCurrent가 없는 요청(대외활동)은 항상 false. */
public interface PeriodRequest {

	YearMonth startYm();

	YearMonth endYm();

	default Boolean isCurrent() {
		return false;
	}
}
