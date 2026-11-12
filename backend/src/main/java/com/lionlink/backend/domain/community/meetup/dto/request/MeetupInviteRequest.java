package com.lionlink.backend.domain.community.meetup.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MeetupInviteRequest(
		@NotEmpty(message = "초대할 동문을 선택해 주세요.")
		@Size(max = 20, message = "한 번에 최대 20명까지 초대할 수 있어요.")
		List<@NotNull Long> userIds
) {
}
