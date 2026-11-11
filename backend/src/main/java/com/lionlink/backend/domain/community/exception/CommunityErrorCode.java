package com.lionlink.backend.domain.community.exception;

import com.lionlink.backend.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommunityErrorCode implements ErrorCode {

	POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없어요."),
	NOT_OWNER(HttpStatus.FORBIDDEN, "본인이 작성한 글만 수정·삭제할 수 있어요."),
	ANONYMOUS_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "번개모임과 커피챗 모집은 익명으로 작성할 수 없어요."),

	COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없어요."),
	PARENT_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "답글을 작성할 댓글을 찾을 수 없어요."),

	MEETUP_NOT_FOUND(HttpStatus.NOT_FOUND, "번개모임을 찾을 수 없어요."),
	MEETUP_CLOSED(HttpStatus.CONFLICT, "참여할 수 없는 번개모임이에요."),
	MEETUP_FULL(HttpStatus.CONFLICT, "모집 인원이 마감되었어요."),
	ALREADY_JOINED(HttpStatus.CONFLICT, "이미 참여 중인 번개모임이에요."),
	NOT_MEETUP_MEMBER(HttpStatus.NOT_FOUND, "참여 중인 번개모임이 아니에요."),
	HOST_CANNOT_LEAVE(HttpStatus.FORBIDDEN, "모임 작성자는 참여 취소 대신 모임 취소를 이용해 주세요."),

	KEYWORD_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "인기 키워드를 불러오지 못했어요."),
	INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식이에요."),
	FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "업로드 가능한 파일 크기를 초과했어요.");

	private final HttpStatus status;
	private final String message;
}
