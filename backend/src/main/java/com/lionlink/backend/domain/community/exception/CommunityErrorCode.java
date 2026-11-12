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
	ANONYMOUS_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "익명 글은 자유 게시판에서만 쓸 수 있어요."),
	INVALID_CATEGORY_DETAIL(HttpStatus.BAD_REQUEST, "카테고리에 맞는 정보를 입력해 주세요."),
	CATEGORY_CHANGE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "번개·커피챗 모집 글은 카테고리를 바꿀 수 없어요."),
	INVALID_MEETUP_TIME(HttpStatus.BAD_REQUEST, "번개 일정은 1시간 뒤부터 14일 이내로 정해 주세요."),
	INVALID_COMPANIONS(HttpStatus.BAD_REQUEST, "함께하는 동문을 다시 확인해 주세요."),
	INVALID_RECRUIT_SENIOR(HttpStatus.BAD_REQUEST, "본인은 신청할 선배로 고를 수 없어요."),
	GROUP_COFFEE_CHAT_NOT_ALLOWED(HttpStatus.CONFLICT, "이 선배는 지금 1:1 커피챗만 받아요."),
	RECRUIT_CLOSED(HttpStatus.CONFLICT, "참여할 수 없는 커피챗 모집이에요."),
	RECRUIT_SENIOR_LOCKED(HttpStatus.CONFLICT, "참여자가 있어 신청할 선배를 바꿀 수 없어요."),
	CAPACITY_BELOW_MEMBERS(HttpStatus.CONFLICT, "지금 참여 인원보다 적게 정할 수 없어요."),
	RECRUIT_NOT_FOUND(HttpStatus.NOT_FOUND, "커피챗 모집 글을 찾을 수 없어요."),
	SENIOR_CANNOT_JOIN(HttpStatus.FORBIDDEN, "신청받을 사자는 모집에 참여할 수 없어요."),
	RECRUIT_FULL(HttpStatus.CONFLICT, "모집 인원이 마감되었어요."),
	OPEN_COFFEE_CHAT_EXISTS(HttpStatus.CONFLICT, "이 사자와 이미 진행 중인 커피챗이 있어요."),

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
