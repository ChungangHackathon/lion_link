package com.lionlink.backend.domain.profile;

import static org.assertj.core.api.Assertions.assertThat;

import com.lionlink.backend.domain.coffeechat.entity.CoffeePeriod;
import com.lionlink.backend.domain.coffeechat.entity.ReceiveMode;
import com.lionlink.backend.domain.profile.CoffeeAvailability.Availability;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CoffeeAvailabilityTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 12);

	@Test
	void 받기_ON이고_쉬는_기간이_없으면_받는_중() {
		CoffeeAvailability result = calculate(ReceiveMode.ON, period(-10, -5), period(3, 5));

		assertThat(result.availability()).isEqualTo(Availability.AVAILABLE);
		assertThat(result.nextAvailableDate()).isNull();
		assertThat(result.available()).isTrue();
	}

	@Test
	void 받기_ON이고_쉬는_기간_안이면_종료일_다음_날부터_받는다() {
		assertRestingUntil(calculate(ReceiveMode.ON, period(-2, 3)), 4);
		assertRestingUntil(calculate(ReceiveMode.ON, period(0, 3)), 4);
		assertRestingUntil(calculate(ReceiveMode.ON, period(-3, 0)), 1);
	}

	@Test
	void 쉬는_기간이_바로_이어지면_마지막_기간이_끝난_다음_날부터_받는다() {
		CoffeeAvailability result = calculate(ReceiveMode.ON, period(6, 9), period(-1, 2), period(3, 5), period(12, 15));

		assertRestingUntil(result, 10);
	}

	@Test
	void 받기_OFF이고_받는_기간_안이면_받는_중() {
		CoffeeAvailability result = calculate(ReceiveMode.OFF, period(0, 0));

		assertThat(result.availability()).isEqualTo(Availability.AVAILABLE);
		assertThat(result.nextAvailableDate()).isNull();
	}

	@Test
	void 받기_OFF이고_받는_기간_밖이면_다음_받는_기간_시작일을_알려준다() {
		CoffeeAvailability result = calculate(ReceiveMode.OFF, period(10, 12), period(-5, -1), period(2, 4));

		assertThat(result.availability()).isEqualTo(Availability.NOT_ACCEPTING);
		assertThat(result.nextAvailableDate()).isEqualTo(TODAY.plusDays(2));
	}

	@Test
	void 받기_OFF이고_다음_받는_기간이_없으면_날짜가_없다() {
		CoffeeAvailability result = calculate(ReceiveMode.OFF, period(-5, -1));

		assertThat(result.availability()).isEqualTo(Availability.NOT_ACCEPTING);
		assertThat(result.nextAvailableDate()).isNull();
	}

	@Test
	void 여러_명_신청은_받는_중이면서_여러_명을_받을_때만_가능하다() {
		assertThat(CoffeeAvailability.of(ReceiveMode.ON, true, List.of(), TODAY).groupAvailable()).isTrue();
		assertThat(CoffeeAvailability.of(ReceiveMode.ON, false, List.of(), TODAY).groupAvailable()).isFalse();
		assertThat(CoffeeAvailability.of(ReceiveMode.ON, true, List.of(period(0, 1)), TODAY).groupAvailable()).isFalse();
		assertThat(CoffeeAvailability.NOT_ACCEPTING.groupAvailable()).isFalse();
	}

	private CoffeeAvailability calculate(ReceiveMode mode, CoffeePeriod... periods) {
		return CoffeeAvailability.of(mode, true, List.of(periods), TODAY);
	}

	private CoffeePeriod period(int startOffset, int endOffset) {
		return CoffeePeriod.builder()
				.userId(1L)
				.startDate(TODAY.plusDays(startOffset))
				.endDate(TODAY.plusDays(endOffset))
				.build();
	}

	private void assertRestingUntil(CoffeeAvailability result, int nextOffset) {
		assertThat(result.availability()).isEqualTo(Availability.RESTING);
		assertThat(result.nextAvailableDate()).isEqualTo(TODAY.plusDays(nextOffset));
		assertThat(result.available()).isFalse();
	}
}
