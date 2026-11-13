package com.lionlink.backend.domain.profile.section.dto.request;

import com.lionlink.backend.domain.profile.section.lionproject.LionEventType;
import com.lionlink.backend.domain.profile.validation.HttpUrl;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** PROFILE-11 멋사 프로젝트·해커톤 추가·수정. */
public record LionProjectRequest(
		@NotBlank(message = "프로젝트 이름을 입력해 주세요.")
		@Size(max = 30, message = "프로젝트 이름은 30자 이하로 입력해 주세요.")
		String name,

		@NotNull(message = "종류를 선택해 주세요.")
		LionEventType eventType,

		@NotNull(message = "기수를 입력해 주세요.")
		@Min(value = 1, message = "기수는 1~14 사이로 입력해 주세요.")
		@Max(value = 14, message = "기수는 1~14 사이로 입력해 주세요.")
		Integer generation,

		@Size(max = 30, message = "맡은 역할은 30자 이하로 입력해 주세요.")
		String myRole,

		@Size(max = 200, message = "설명은 200자 이하로 입력해 주세요.")
		String description,

		@HttpUrl
		String link
) {
}
