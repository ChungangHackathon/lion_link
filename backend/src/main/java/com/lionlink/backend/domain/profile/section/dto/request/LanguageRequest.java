package com.lionlink.backend.domain.profile.section.dto.request;

import com.lionlink.backend.domain.profile.section.language.LanguageLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** PROFILE-11 언어 추가·수정. 같은 언어는 대소문자를 무시하고 한 번만. */
public record LanguageRequest(
		@NotBlank(message = "언어를 입력해 주세요.")
		@Size(max = 20, message = "언어는 20자 이하로 입력해 주세요.")
		String language,

		@NotNull(message = "수준을 선택해 주세요.")
		LanguageLevel level
) {
}
