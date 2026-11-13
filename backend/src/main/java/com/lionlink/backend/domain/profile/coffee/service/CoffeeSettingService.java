package com.lionlink.backend.domain.profile.coffee.service;

import static com.lionlink.backend.domain.profile.section.service.SectionItemSupport.blankToNull;

import com.lionlink.backend.domain.coffeechat.entity.AvailableSlot;
import com.lionlink.backend.domain.coffeechat.entity.CoffeePeriod;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeSetting;
import com.lionlink.backend.domain.coffeechat.entity.ReceiveMode;
import com.lionlink.backend.domain.coffeechat.entity.SlotDay;
import com.lionlink.backend.domain.coffeechat.repository.CoffeePeriodRepository;
import com.lionlink.backend.domain.coffeechat.repository.CoffeeSettingRepository;
import com.lionlink.backend.domain.profile.coffee.CoffeeTopic;
import com.lionlink.backend.domain.profile.coffee.dto.request.CoffeeSettingUpdateRequest.SlotInput;
import com.lionlink.backend.domain.profile.coffee.dto.request.CoffeeSettingUpdateRequest;
import com.lionlink.backend.domain.profile.coffee.dto.request.CoffeeSwitchRequest.RestFor;
import com.lionlink.backend.domain.profile.coffee.dto.request.CoffeeSwitchRequest;
import com.lionlink.backend.domain.profile.coffee.dto.response.CoffeeSettingResponse;
import com.lionlink.backend.domain.profile.exception.ProfileErrorCode;
import com.lionlink.backend.domain.profile.section.service.SectionItemSupport;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.CommonErrorCode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PROFILE-12~14 커피챗 받기 설정 조회·저장·홈 스위치. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoffeeSettingService {

	private final UserRepository userRepository;
	private final CoffeeSettingRepository coffeeSettingRepository;
	private final CoffeePeriodRepository coffeePeriodRepository;
	private final CoffeePeriodService coffeePeriodService;
	private final SectionItemSupport support;

	/** PROFILE-12: 지난 기간을 뺀 설정과 오늘 기준 상태. */
	public CoffeeSettingResponse get(Long userId) {
		if (!userRepository.existsByIdAndDeletedAtIsNull(userId)) {
			throw new BusinessException(CommonErrorCode.UNAUTHENTICATED);
		}
		return toResponse(findSetting(userId), userId, CoffeePeriodService.today());
	}

	/** PROFILE-13: 기본 상태를 바꾸면 기간이 모두 지워진다. 기간이 있는데 확인(confirmClearPeriods) 없이 바꾸면 409. */
	@Transactional
	public CoffeeSettingResponse update(Long userId, CoffeeSettingUpdateRequest request) {
		support.lockProfile(userId);
		CoffeeSetting setting = findSetting(userId);
		LocalDate today = CoffeePeriodService.today();

		if (request.defaultMode() != setting.getDefaultMode()) {
			changeDefaultMode(setting, request.defaultMode(), request.confirmClearPeriods(), today);
		}
		setting.update(
				request.acceptGroup(),
				request.topics() == null ? List.of() : request.topics().stream().map(CoffeeTopic::name).toList(),
				List.copyOf(request.modes()),
				toSlots(request.availableSlots()),
				blankToNull(request.preferredArea()));
		return toResponse(setting, userId, today);
	}

	/**
	 * PROFILE-14: 지금 상태에 따라 알아서 처리한다. 이미 원하는 상태면 아무것도 바꾸지 않고 그대로 200.
	 * 받는 중 = (기본 ON이고 쉬는 기간 밖) 또는 (기본 OFF이고 받는 기간 안).
	 */
	@Transactional
	public CoffeeSettingResponse switchReceiving(Long userId, CoffeeSwitchRequest request) {
		support.lockProfile(userId);
		CoffeeSetting setting = findSetting(userId);
		LocalDate today = CoffeePeriodService.today();
		List<CoffeePeriod> active = coffeePeriodService.activePeriods(userId, today);

		boolean modeOn = setting.getDefaultMode() == ReceiveMode.ON;
		boolean inPeriod = active.stream().anyMatch(period -> !period.getStartDate().isAfter(today));
		boolean receiving = modeOn != inPeriod;
		boolean wantOn = Boolean.TRUE.equals(request.on());

		if (wantOn != receiving) {
			if (wantOn && !modeOn) {
				// 안 받는 중(OFF, 기간 밖) 켜기 → 기본 상태를 ON으로
				changeDefaultMode(setting, ReceiveMode.ON, request.confirmClearPeriods(), today);
			} else if (wantOn || !modeOn) {
				// 쉬는 중(ON, 쉬는 기간 안) 켜기, 받는 중(OFF, 받는 기간 안) 끄기 → 진행 중인 기간을 끝냄
				coffeePeriodService.endRunningPeriods(active, today);
			} else {
				rest(userId, setting, request, today);
			}
		}
		return toResponse(setting, userId, today);
	}

	/** 받는 중(기본 ON)에서 끄기. 쉬는 기간을 만들거나 기본 상태를 OFF로 바꾼다. */
	private void rest(Long userId, CoffeeSetting setting, CoffeeSwitchRequest request, LocalDate today) {
		RestFor restFor = request.restFor();
		if (restFor == null) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR, "쉬는 기간을 골라 주세요.");
		}
		if (restFor == RestFor.UNTIL_RESUME) {
			changeDefaultMode(setting, ReceiveMode.OFF, request.confirmClearPeriods(), today);
			return;
		}
		LocalDate start = restFor == RestFor.CUSTOM ? request.startDate() : today;
		LocalDate end = switch (restFor) {
			case WEEK_1 -> today.plusDays(6);
			case WEEK_2 -> today.plusDays(13);
			case MONTH_1 -> today.plusMonths(1).minusDays(1);
			default -> request.endDate();
		};
		// 한 번에 눌러도 실패하지 않게 겹치는 쉬는 기간은 합친다.
		coffeePeriodService.addPeriod(userId, start, end, true, today);
	}

	private void changeDefaultMode(CoffeeSetting setting, ReceiveMode mode, Boolean confirmClearPeriods, LocalDate today) {
		Long userId = setting.getUserId();
		if (!coffeePeriodService.activePeriods(userId, today).isEmpty() && !Boolean.TRUE.equals(confirmClearPeriods)) {
			throw new BusinessException(ProfileErrorCode.PERIODS_WILL_BE_CLEARED);
		}
		coffeePeriodRepository.deleteByUserId(userId);
		setting.changeDefaultMode(mode);
	}

	private CoffeeSetting findSetting(Long userId) {
		return coffeeSettingRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.PROFILE_NOT_COMPLETED));
	}

	private CoffeeSettingResponse toResponse(CoffeeSetting setting, Long userId, LocalDate today) {
		return CoffeeSettingResponse.of(setting, coffeePeriodService.activePeriods(userId, today), today);
	}

	/** 같은 요일은 두 번 받지 않는다. 요일 순서, 시간대 순서로 정렬해 저장한다. */
	private static List<AvailableSlot> toSlots(List<SlotInput> inputs) {
		if (inputs == null) {
			return List.of();
		}
		Set<SlotDay> days = new HashSet<>();
		for (SlotInput input : inputs) {
			if (!days.add(input.day())) {
				throw new BusinessException(CommonErrorCode.VALIDATION_ERROR, "같은 요일을 두 번 입력할 수 없어요.");
			}
		}
		return inputs.stream()
				.sorted(Comparator.comparing(SlotInput::day))
				.map(input -> new AvailableSlot(input.day(), input.times().stream().sorted().toList()))
				.toList();
	}
}
