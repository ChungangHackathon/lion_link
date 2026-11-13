package com.lionlink.backend.domain.profile.section.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.YearMonth;

/** PROFILE-11 자격증 추가·수정. */
public record CertificationRequest(
		@NotBlank(message = "자격증 이름을 입력해 주세요.")
		@Size(max = 30, message = "자격증 이름은 30자 이하로 입력해 주세요.")
		String name,

		@Size(max = 30, message = "발급 기관은 30자 이하로 입력해 주세요.")
		String issuer,

		YearMonth acquiredYm
) {
}
