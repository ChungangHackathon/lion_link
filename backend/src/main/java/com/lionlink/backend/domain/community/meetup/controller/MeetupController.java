package com.lionlink.backend.domain.community.meetup.controller;

import com.lionlink.backend.domain.community.meetup.dto.request.MeetupInviteRequest;
import com.lionlink.backend.domain.community.meetup.dto.response.MeetupInviteResponse;
import com.lionlink.backend.domain.community.meetup.dto.response.MeetupParticipationResponse;
import com.lionlink.backend.domain.community.meetup.service.MeetupService;
import com.lionlink.backend.global.error.ErrorResponse;
import com.lionlink.backend.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "번개모임")
@RestController
@RequestMapping("/posts/{postId}/meetup")
@RequiredArgsConstructor
public class MeetupController {

	private final MeetupService meetupService;

	@Operation(summary = "번개 참여", description = "이미 참여 중이면 그대로 성공한다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "404", description = "게시글 또는 번개 없음 (POST_NOT_FOUND, MEETUP_NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "참여할 수 없는 번개 (MEETUP_CLOSED, MEETUP_FULL)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PutMapping("/members/me")
	public MeetupParticipationResponse join(
			@CurrentUser Long userId,
			@Parameter(description = "번개 게시글 ID") @PathVariable Long postId
	) {
		return meetupService.join(userId, postId);
	}

	@Operation(summary = "번개 참여 취소", description = "작성자는 참여 취소 대신 게시글 삭제로 번개를 취소한다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "403", description = "작성자는 참여 취소 불가 (HOST_CANNOT_LEAVE)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "참여 중이 아님 (NOT_MEETUP_MEMBER, MEETUP_NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "종료되거나 취소된 번개 (MEETUP_CLOSED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@DeleteMapping("/members/me")
	public MeetupParticipationResponse leave(
			@CurrentUser Long userId,
			@Parameter(description = "번개 게시글 ID") @PathVariable Long postId
	) {
		return meetupService.leave(userId, postId);
	}

	@Operation(summary = "번개 초대",
			description = "이미 참여했거나 초대한 회원, 승인되지 않은 회원은 건너뛰고 skippedUserIds로 알려준다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "400", description = "초대 대상 오류 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "403", description = "작성자가 아님 (NOT_OWNER)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "종료되거나 취소된 번개 (MEETUP_CLOSED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/invites")
	public MeetupInviteResponse invite(
			@CurrentUser Long userId,
			@Parameter(description = "번개 게시글 ID") @PathVariable Long postId,
			@Valid @RequestBody MeetupInviteRequest request
	) {
		return meetupService.invite(userId, postId, request);
	}
}
