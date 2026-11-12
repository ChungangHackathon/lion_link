package com.lionlink.backend.domain.community.post.dto.request;

import com.lionlink.backend.domain.community.post.entity.PostCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PostCreateRequest(
		@NotNull(message = "카테고리를 선택해 주세요.")
		PostCategory category,

		@NotBlank(message = "제목을 입력해 주세요.")
		@Size(min = 2, max = 50, message = "제목은 2~50자여야 합니다.")
		String title,

		@NotBlank(message = "본문을 입력해 주세요.")
		@Size(max = 5000, message = "본문은 5000자 이하여야 합니다.")
		String content,

		@Size(max = 3, message = "링크는 최대 3개까지 넣을 수 있어요.")
		List<@Pattern(regexp = PostRequestRules.LINK, message = PostRequestRules.LINK_MESSAGE)
				@Size(max = 500, message = "링크는 500자 이하여야 합니다.") String> links,

		Boolean isAnonymous,

		@FutureOrPresent(message = "마감일은 오늘 이후여야 합니다.")
		LocalDate deadline,

		@Valid
		MeetupRequest meetup,

		@Valid
		RecruitRequest recruit
) {

	public boolean anonymous() {
		return Boolean.TRUE.equals(isAnonymous);
	}

	public record MeetupRequest(
			@NotNull(message = "모임 일시를 입력해 주세요.")
			LocalDateTime meetAt,

			@NotBlank(message = "장소를 입력해 주세요.")
			@Size(max = 50, message = "장소는 50자 이하여야 합니다.")
			String place,

			@NotNull(message = "정원을 입력해 주세요.")
			@Min(value = 2, message = "정원은 2~30명이어야 합니다.")
			@Max(value = 30, message = "정원은 2~30명이어야 합니다.")
			Integer capacity,

			List<Long> companionIds
	) {
	}

	public record RecruitRequest(
			@NotNull(message = "신청할 사자를 선택해 주세요.")
			Long seniorId,

			@NotNull(message = "모집 인원을 입력해 주세요.")
			@Min(value = 2, message = "모집 인원은 2~5명이어야 합니다.")
			@Max(value = 5, message = "모집 인원은 2~5명이어야 합니다.")
			Integer capacity,

			@NotNull(message = "모집 마감일을 입력해 주세요.")
			@FutureOrPresent(message = "마감일은 오늘 이후여야 합니다.")
			LocalDate deadline
	) {
	}
}
