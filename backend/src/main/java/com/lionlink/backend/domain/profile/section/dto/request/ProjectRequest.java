package com.lionlink.backend.domain.profile.section.dto.request;

import com.lionlink.backend.domain.profile.validation.DistinctItems;
import com.lionlink.backend.domain.profile.validation.HttpUrl;
import com.lionlink.backend.domain.profile.validation.PeriodRequest;
import com.lionlink.backend.domain.profile.validation.ValidPeriod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.YearMonth;
import java.util.List;

/** PROFILE-11 프로젝트 추가·수정. 태그는 최대 5개, 항목당 1~20자, 중복 불가. */
@ValidPeriod
public record ProjectRequest(
		@NotBlank(message = "프로젝트 이름을 입력해 주세요.")
		@Size(max = 30, message = "프로젝트 이름은 30자 이하로 입력해 주세요.")
		String name,

		YearMonth startYm,

		YearMonth endYm,

		Boolean isCurrent,

		@Size(max = 30, message = "역할은 30자 이하로 입력해 주세요.")
		String role,

		@Size(max = 300, message = "설명은 300자 이하로 입력해 주세요.")
		String description,

		@HttpUrl
		String link,

		@Size(max = 5, message = "태그는 최대 5개까지 입력할 수 있어요.")
		@DistinctItems(message = "같은 태그를 두 번 입력할 수 없어요.")
		List<@NotBlank(message = "태그를 입력해 주세요.") @Size(max = 20, message = "태그는 20자 이하로 입력해 주세요.") String> tags
) implements PeriodRequest {
}
