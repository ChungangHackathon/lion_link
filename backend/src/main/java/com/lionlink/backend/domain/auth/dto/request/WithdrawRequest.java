package com.lionlink.backend.domain.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** 회원 탈퇴(AUTH-08). 확인 문구가 정확히 "탈퇴"여야 한다(공백도 허용하지 않는다). */
public record WithdrawRequest(
		@NotNull(message = "확인 문구로 '탈퇴'를 입력해 주세요.")
		@Pattern(regexp = "탈퇴", message = "확인 문구로 '탈퇴'를 입력해 주세요.") String confirm
) {
}
