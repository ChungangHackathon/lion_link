package com.lionlink.backend.domain.profile;

import com.lionlink.backend.domain.coffeechat.entity.CoffeePeriod;
import com.lionlink.backend.domain.coffeechat.entity.ReceiveMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public record CoffeeAvailability(
		Availability availability,
		LocalDate nextAvailableDate,
		boolean acceptGroup
) {

	public static final CoffeeAvailability NOT_ACCEPTING = new CoffeeAvailability(Availability.NOT_ACCEPTING, null, false);

	public static CoffeeAvailability of(ReceiveMode defaultMode, boolean acceptGroup, List<CoffeePeriod> periods, LocalDate today) {
		List<CoffeePeriod> remaining = periods.stream()
				.filter(period -> !period.getEndDate().isBefore(today))
				.sorted(Comparator.comparing(CoffeePeriod::getStartDate))
				.toList();
		Optional<CoffeePeriod> current = remaining.stream()
				.filter(period -> !period.getStartDate().isAfter(today))
				.findFirst();

		if (defaultMode == ReceiveMode.ON) {
			return current
					.map(period -> new CoffeeAvailability(Availability.RESTING, restEnd(remaining, period).plusDays(1), acceptGroup))
					.orElseGet(() -> new CoffeeAvailability(Availability.AVAILABLE, null, acceptGroup));
		}
		if (current.isPresent()) {
			return new CoffeeAvailability(Availability.AVAILABLE, null, acceptGroup);
		}
		LocalDate nextOpen = remaining.stream().map(CoffeePeriod::getStartDate).findFirst().orElse(null);
		return new CoffeeAvailability(Availability.NOT_ACCEPTING, nextOpen, acceptGroup);
	}

	private static LocalDate restEnd(List<CoffeePeriod> periods, CoffeePeriod current) {
		LocalDate end = current.getEndDate();
		for (CoffeePeriod period : periods) {
			if (!period.getStartDate().isAfter(end.plusDays(1)) && period.getEndDate().isAfter(end)) {
				end = period.getEndDate();
			}
		}
		return end;
	}

	public boolean available() {
		return availability == Availability.AVAILABLE;
	}

	public boolean groupAvailable() {
		return available() && acceptGroup;
	}

	public enum Availability {
		AVAILABLE,
		RESTING,
		NOT_ACCEPTING
	}
}
