package com.lionlink.backend.domain.coffeechat.dto.request;

import com.lionlink.backend.domain.coffeechat.entity.MeetingMode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CoffeeChatScheduleRequest(
		@NotNull(message = "만남 일시를 입력해 주세요.")
		@Future(message = "지금 이후 시간으로 골라 주세요.")
		LocalDateTime scheduledAt,

		@NotNull(message = "만남 방식을 골라 주세요.")
		MeetingMode meetingMode,

		@Size(max = 50, message = "장소는 50자 이하여야 합니다.")
		String meetingPlace,

		@Size(max = 500, message = "링크는 500자 이하여야 합니다.")
		@Pattern(regexp = "^https?://\\S+$", message = "링크는 http:// 또는 https://로 시작해야 해요.")
		String meetingLink
) {

	@AssertTrue(message = "오프라인 만남은 장소를 입력해 주세요.")
	public boolean isPlaceGivenForOffline() {
		return meetingMode != MeetingMode.OFFLINE || (meetingPlace != null && !meetingPlace.isBlank());
	}
}
