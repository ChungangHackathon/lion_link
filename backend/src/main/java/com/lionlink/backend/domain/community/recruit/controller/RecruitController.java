package com.lionlink.backend.domain.community.recruit.controller;

import com.lionlink.backend.domain.community.recruit.dto.response.RecruitParticipationResponse;
import com.lionlink.backend.domain.community.recruit.service.RecruitService;
import com.lionlink.backend.global.error.ErrorResponse;
import com.lionlink.backend.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "커피챗 모집")
@RestController
@RequestMapping("/posts/{postId}/recruit")
@RequiredArgsConstructor
public class RecruitController {

	private final RecruitService recruitService;

	@Operation(summary = "커피챗 모집 참여", description = "이미 참여 중이면 그대로 성공한다. 참여하면 모집 작성자에게 알림이 간다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "403", description = "신청받을 사자 본인 (SENIOR_CANNOT_JOIN)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "게시글 또는 커피챗 모집 없음 (POST_NOT_FOUND, RECRUIT_NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "참여할 수 없는 모집 (RECRUIT_CLOSED, RECRUIT_FULL, OPEN_COFFEE_CHAT_EXISTS)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PutMapping("/members/me")
	public RecruitParticipationResponse join(
			@CurrentUser Long userId,
			@Parameter(description = "커피챗 모집 게시글 ID") @PathVariable Long postId
	) {
		return recruitService.join(userId, postId);
	}
}
