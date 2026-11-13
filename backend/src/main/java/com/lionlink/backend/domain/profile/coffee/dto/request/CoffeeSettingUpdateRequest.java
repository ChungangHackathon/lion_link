package com.lionlink.backend.domain.profile.coffee.dto.request;

import com.lionlink.backend.domain.coffeechat.entity.MeetingMode;
import com.lionlink.backend.domain.coffeechat.entity.ReceiveMode;
import com.lionlink.backend.domain.coffeechat.entity.SlotDay;
import com.lionlink.backend.domain.coffeechat.entity.SlotTime;
import com.lionlink.backend.domain.profile.coffee.CoffeeTopic;
import com.lionlink.backend.domain.profile.validation.DistinctItems;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** PROFILE-13. 보낸 값으로 통째로 바꾼다. 기본 상태를 바꾸면 기간이 모두 지워지므로 기간이 있을 땐 confirmClearPeriods가 true여야 한다. */
public record CoffeeSettingUpdateRequest(
		@NotNull(message = "기본 상태를 선택해 주세요.")
		ReceiveMode defaultMode,

		@NotNull(message = "여러 명 커피챗 여부를 선택해 주세요.")
		Boolean acceptGroup,

		@DistinctItems(message = "같은 주제를 두 번 고를 수 없어요.")
		List<@NotNull(message = "주제를 선택해 주세요.") CoffeeTopic> topics,

		@NotEmpty(message = "가능한 방식을 1개 이상 골라주세요.")
		@DistinctItems(message = "같은 방식을 두 번 고를 수 없어요.")
		List<@NotNull(message = "방식을 선택해 주세요.") MeetingMode> modes,

		List<@NotNull(message = "가능한 시간을 입력해 주세요.") @Valid SlotInput> availableSlots,

		@Size(max = 30, message = "선호 지역은 30자 이하로 입력해 주세요.")
		String preferredArea,

		Boolean confirmClearPeriods
) {

	public record SlotInput(
			@NotNull(message = "요일을 선택해 주세요.")
			SlotDay day,

			@NotEmpty(message = "시간대를 1개 이상 골라주세요.")
			@DistinctItems(message = "같은 시간대를 두 번 고를 수 없어요.")
			List<@NotNull(message = "시간대를 선택해 주세요.") SlotTime> times
	) {
	}
}
