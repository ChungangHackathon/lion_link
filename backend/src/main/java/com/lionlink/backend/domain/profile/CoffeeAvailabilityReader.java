package com.lionlink.backend.domain.profile;

import com.lionlink.backend.domain.coffeechat.entity.CoffeePeriod;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeSetting;
import com.lionlink.backend.domain.coffeechat.repository.CoffeePeriodRepository;
import com.lionlink.backend.domain.coffeechat.repository.CoffeeSettingRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoffeeAvailabilityReader {

	private final CoffeeSettingRepository coffeeSettingRepository;
	private final CoffeePeriodRepository coffeePeriodRepository;

	public Map<Long, CoffeeAvailability> getAvailabilities(Collection<Long> userIds) {
		Map<Long, CoffeeAvailability> availabilities = new HashMap<>();
		if (userIds.isEmpty()) {
			return availabilities;
		}
		LocalDate today = LocalDate.now();
		Map<Long, List<CoffeePeriod>> periods = coffeePeriodRepository.findByUserIdInAndEndDateGreaterThanEqual(userIds, today)
				.stream()
				.collect(Collectors.groupingBy(CoffeePeriod::getUserId));

		userIds.forEach(userId -> availabilities.put(userId, CoffeeAvailability.NOT_ACCEPTING));
		for (CoffeeSetting setting : coffeeSettingRepository.findAllById(userIds)) {
			availabilities.put(setting.getUserId(), CoffeeAvailability.of(setting.getDefaultMode(), setting.isAcceptGroup(),
					periods.getOrDefault(setting.getUserId(), List.of()), today));
		}
		return availabilities;
	}

	public CoffeeAvailability getAvailability(Long userId) {
		return getAvailabilities(List.of(userId)).get(userId);
	}
}
