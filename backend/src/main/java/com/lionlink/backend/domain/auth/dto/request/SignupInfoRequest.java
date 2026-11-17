package com.lionlink.backend.domain.auth.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 가입 정보 수정(AUTH-06)과 다시 신청(AUTH-07)이 같이 쓴다. */
public record SignupInfoRequest(
		@NotBlank(message = "이름을 입력해 주세요.")
		@Size(min = 2, max = 20, message = "이름은 2~20자로 입력해 주세요.") String name,
		@NotNull(message = "기수를 선택해 주세요.")
		@Min(value = 1, message = "기수는 1 이상이어야 해요.")
		@Max(value = 14, message = "기수는 14 이하여야 해요.") Integer generation
) {
}
