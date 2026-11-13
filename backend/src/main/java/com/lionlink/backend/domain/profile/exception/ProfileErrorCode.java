package com.lionlink.backend.domain.profile.exception;

import com.lionlink.backend.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProfileErrorCode implements ErrorCode {

	DUPLICATE_ITEM(HttpStatus.CONFLICT, "이미 등록된 항목이에요."),
	ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 기본 정보를 입력했어요."),
	LIMIT_EXCEEDED(HttpStatus.CONFLICT, "더 이상 추가할 수 없어요. 최대 개수를 넘었어요."),
	DEFAULT_ITEM_NOT_DELETABLE(HttpStatus.FORBIDDEN, "가입 기수 항목은 삭제할 수 없어요."),
	PERIODS_WILL_BE_CLEARED(HttpStatus.CONFLICT, "등록한 기간이 모두 지워져요. 확인 후 다시 시도해 주세요."),
	PERIOD_OVERLAP(HttpStatus.CONFLICT, "이미 등록한 기간과 겹쳐요."),
	PERIOD_LIMIT(HttpStatus.CONFLICT, "기간은 최대 5개까지 등록할 수 있어요."),
	UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "jpg, png, webp 사진만 올릴 수 있어요."),
	PHOTO_UPLOAD_FAILED(HttpStatus.BAD_GATEWAY, "사진을 올리지 못했어요. 잠시 후 다시 시도해 주세요.");

	private final HttpStatus status;
	private final String message;
}
