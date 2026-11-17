package com.lionlink.backend.domain.profile.basic.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** PROFILE-09. 이메일은 다른 동문에게 보여주는 용도라 가입 이메일과 달라도 된다. 전화번호는 비우면 삭제. */
public record ContactUpdateRequest(
		@NotBlank(message = "이메일을 입력해 주세요.")
		@Email(message = "올바른 이메일 형식으로 입력해 주세요.")
		@Size(max = 255, message = "이메일은 255자 이하로 입력해 주세요.")
		String email,

		@Pattern(regexp = "^(010-[0-9]{4}-[0-9]{4})?$", message = "전화번호는 010-0000-0000 형식으로 입력해 주세요.")
		String phone
) {
}
