package com.lionlink.backend.domain.coffeechat.dto.request;

import com.lionlink.backend.domain.coffeechat.entity.PreferredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CoffeeChatCreateRequest(
		Long receiverId,

		@NotBlank(message = "상담 주제를 골라 주세요.")
		@Size(max = 30, message = "상담 주제는 30자 이하여야 합니다.")
		String topic,

		@Size(max = 30, message = "직접 입력한 주제는 30자 이하여야 합니다.")
		String topicCustom,

		@NotBlank(message = "신청 이유를 입력해 주세요.")
		@Size(min = 10, max = 500, message = "신청 이유는 10~500자여야 합니다.")
		String reason,

		@Size(max = 3, message = "질문은 최대 3개까지 넣을 수 있어요.")
		List<@NotBlank(message = "질문을 입력해 주세요.") @Size(min = 5, max = 200, message = "질문은 5~200자여야 합니다.") String> questions,

		@NotNull(message = "희망 방식을 골라 주세요.")
		PreferredMode preferredMode,

		@Size(max = 100, message = "희망 일정 메모는 100자 이하여야 합니다.")
		String scheduleNote,

		@Size(max = 4, message = "함께 신청할 동문은 최대 4명이에요.")
		List<@NotNull Long> memberIds,

		Long fromRecruitId
) {
}
