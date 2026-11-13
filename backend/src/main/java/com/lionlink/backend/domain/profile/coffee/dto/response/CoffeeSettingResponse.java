package com.lionlink.backend.domain.profile.coffee.dto.response;

import com.lionlink.backend.domain.coffeechat.entity.AvailableSlot;
import com.lionlink.backend.domain.coffeechat.entity.CoffeePeriod;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeSetting;
import com.lionlink.backend.domain.coffeechat.entity.MeetingMode;
import com.lionlink.backend.domain.coffeechat.entity.ReceiveMode;
import com.lionlink.backend.domain.profile.CoffeeAvailability;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** PROFILE-12 커피챗 받기 설정. PROFILE-13·14 응답도 같은 모양이다. */
public record CoffeeSettingResponse(
		String defaultMode,
		boolean acceptGroup,
		List<String> topics,
		List<String> modes,
		List<AvailableSlot> availableSlots,
		String preferredArea,
		String periodKind,
		List<CoffeePeriodResponse> periods,
		Status status
) {

	public record Status(String availability, LocalDate nextAvailableDate) {
	}

	/** activePeriods는 지난 기간을 뺀 내 기간(시작일 순). 상태는 기존 CoffeeAvailability 규칙으로 계산한다. */
	public static CoffeeSettingResponse of(CoffeeSetting setting, List<CoffeePeriod> activePeriods, LocalDate today) {
		CoffeeAvailability availability = CoffeeAvailability.of(
				setting.getDefaultMode(), setting.isAcceptGroup(), activePeriods, today);
		return new CoffeeSettingResponse(
				setting.getDefaultMode().name(),
				setting.isAcceptGroup(),
				Objects.requireNonNullElse(setting.getTopics(), List.of()),
				setting.getModes().stream().map(MeetingMode::name).toList(),
				Objects.requireNonNullElse(setting.getAvailableSlots(), List.of()),
				setting.getPreferredArea(),
				setting.getDefaultMode() == ReceiveMode.ON ? "REST" : "OPEN",
				activePeriods.stream().map(CoffeePeriodResponse::from).toList(),
				new Status(availability.availability().name(), availability.nextAvailableDate()));
	}
}
