package com.lionlink.backend.domain.profile.coffee.service;

import com.lionlink.backend.domain.coffeechat.entity.CoffeePeriod;
import com.lionlink.backend.domain.coffeechat.repository.CoffeePeriodRepository;
import com.lionlink.backend.domain.profile.coffee.dto.request.CoffeePeriodRequest;
import com.lionlink.backend.domain.profile.coffee.dto.response.CoffeePeriodResponse;
import com.lionlink.backend.domain.profile.exception.ProfileErrorCode;
import com.lionlink.backend.domain.profile.section.service.SectionItemSupport;
import com.lionlink.backend.global.config.JacksonConfig;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.CommonErrorCode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PROFILE-15 기간 추가·삭제. 기본 상태가 ON이면 쉬는 기간, OFF면 받는 기간을 뜻한다.
 * 홈 스위치(PROFILE-14)도 같은 규칙을 쓰므로 package-private 메서드를 CoffeeSettingService가 호출한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoffeePeriodService {

	static final int MAX_PERIODS = 5;
	static final int MAX_DAYS = 180;

	private final CoffeePeriodRepository coffeePeriodRepository;
	private final SectionItemSupport support;

	@Transactional
	public CoffeePeriodResponse add(Long userId, CoffeePeriodRequest request) {
		support.lockProfile(userId);
		CoffeePeriod period = addPeriod(userId, request.startDate(), request.endDate(),
				Boolean.TRUE.equals(request.mergeOverlap()), today());
		return CoffeePeriodResponse.from(period);
	}

	@Transactional
	public void delete(Long userId, Long periodId) {
		support.lockProfile(userId);
		CoffeePeriod period = coffeePeriodRepository.findByIdAndUserId(periodId, userId)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));
		coffeePeriodRepository.delete(period);
	}

	/** 날짜 기준은 KST. */
	static LocalDate today() {
		return LocalDate.now(JacksonConfig.ZONE);
	}

	/** 지난 기간을 뺀 내 기간, 시작일이 빠른 순. */
	List<CoffeePeriod> activePeriods(Long userId, LocalDate today) {
		return coffeePeriodRepository.findByUserIdAndEndDateGreaterThanEqualOrderByStartDateAsc(userId, today);
	}

	/**
	 * 기간을 만든다. 겹치는 기간이 있으면 merge가 아닐 때 409, merge일 때는 겹친 기간을 하나로 합친다.
	 * 새 행이 생기는 경우에만 최대 개수(5개, 지난 기간 제외)를 검사한다.
	 */
	CoffeePeriod addPeriod(Long userId, LocalDate startDate, LocalDate endDate, boolean merge, LocalDate today) {
		validateRange(startDate, endDate, today);
		List<CoffeePeriod> active = activePeriods(userId, today);
		List<CoffeePeriod> overlaps = active.stream()
				.filter(period -> !period.getStartDate().isAfter(endDate) && !period.getEndDate().isBefore(startDate))
				.toList();
		if (!overlaps.isEmpty()) {
			if (!merge) {
				throw new BusinessException(ProfileErrorCode.PERIOD_OVERLAP);
			}
			LocalDate mergedStart = overlaps.stream().map(CoffeePeriod::getStartDate)
					.min(Comparator.naturalOrder()).orElseThrow();
			LocalDate mergedEnd = overlaps.stream().map(CoffeePeriod::getEndDate)
					.max(Comparator.naturalOrder()).orElseThrow();
			CoffeePeriod kept = overlaps.get(0);
			kept.changeRange(mergedStart.isBefore(startDate) ? mergedStart : startDate,
					mergedEnd.isAfter(endDate) ? mergedEnd : endDate);
			overlaps.stream().skip(1).forEach(coffeePeriodRepository::delete);
			return kept;
		}
		if (active.size() >= MAX_PERIODS) {
			throw new BusinessException(ProfileErrorCode.PERIOD_LIMIT);
		}
		return coffeePeriodRepository.save(CoffeePeriod.builder()
				.userId(userId).startDate(startDate).endDate(endDate).build());
	}

	/** 종료일은 시작일 이후, 시작일은 오늘부터 1년 이내, 길이는 시작일부터 최대 180일. */
	void validateRange(LocalDate startDate, LocalDate endDate, LocalDate today) {
		if (startDate == null || endDate == null) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR, "시작일과 종료일을 입력해 주세요.");
		}
		if (endDate.isBefore(startDate)) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR, "종료일이 시작일보다 빨라요.");
		}
		if (startDate.isBefore(today)) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR, "시작일은 오늘부터 고를 수 있어요.");
		}
		if (startDate.isAfter(today.plusYears(1))) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR, "시작일은 오늘부터 1년 이내로 골라 주세요.");
		}
		if (ChronoUnit.DAYS.between(startDate, endDate) > MAX_DAYS) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR, "기간은 최대 180일까지 등록할 수 있어요.");
		}
	}

	/**
	 * 지금 진행 중인 기간을 어제까지로 줄이고(오늘 시작한 기간은 지움), 바로 이어지는 뒤쪽 기간도 지운다.
	 * 이어지는 기간을 남기면 내일 다시 같은 상태로 돌아가기 때문이다.
	 */
	void endRunningPeriods(List<CoffeePeriod> active, LocalDate today) {
		List<CoffeePeriod> running = active.stream()
				.filter(period -> !period.getStartDate().isAfter(today))
				.toList();
		if (running.isEmpty()) {
			return;
		}
		LocalDate chainEnd = running.stream().map(CoffeePeriod::getEndDate)
				.max(Comparator.naturalOrder()).orElseThrow();
		for (CoffeePeriod period : running) {
			if (period.getStartDate().isBefore(today)) {
				period.changeRange(period.getStartDate(), today.minusDays(1));
			} else {
				coffeePeriodRepository.delete(period);
			}
		}
		for (CoffeePeriod period : active) {
			if (period.getStartDate().isAfter(today) && !period.getStartDate().isAfter(chainEnd.plusDays(1))) {
				coffeePeriodRepository.delete(period);
				if (period.getEndDate().isAfter(chainEnd)) {
					chainEnd = period.getEndDate();
				}
			}
		}
	}
}
