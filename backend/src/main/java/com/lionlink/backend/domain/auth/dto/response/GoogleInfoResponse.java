package com.lionlink.backend.domain.auth.dto.response;

import com.lionlink.backend.domain.auth.service.GoogleIdentity;

/** 가입 화면에 미리 채울 구글 계정 정보. */
public record GoogleInfoResponse(String email, String name) {

	public static GoogleInfoResponse from(GoogleIdentity identity) {
		return new GoogleInfoResponse(identity.email(), identity.name());
	}
}
