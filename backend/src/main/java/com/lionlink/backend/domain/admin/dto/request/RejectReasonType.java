package com.lionlink.backend.domain.admin.dto.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 반려 사유 유형. ETC가 아니면 고정 문구가 사유로 저장된다. */
@Getter
@RequiredArgsConstructor
public enum RejectReasonType {

	UNVERIFIED("동문인지 확인할 수 없어요"),
	MISMATCH("이름·기수가 맞지 않아요"),
	ETC(null);

	private final String fixedText;
}
