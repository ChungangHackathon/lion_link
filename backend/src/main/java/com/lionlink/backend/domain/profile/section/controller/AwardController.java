package com.lionlink.backend.domain.profile.section.controller;

import com.lionlink.backend.domain.profile.basic.dto.response.ProfileResponse.AwardItem;
import com.lionlink.backend.domain.profile.section.dto.request.AwardRequest;
import com.lionlink.backend.domain.profile.section.service.AwardService;
import com.lionlink.backend.global.error.ErrorResponse;
import com.lionlink.backend.global.security.AllowPendingMember;
import com.lionlink.backend.global.security.CurrentUser;
import com.lionlink.backend.global.security.RequireCompletedProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** PROFILE-11 수상. 승인 대기 회원도 쓸 수 있고 반려 회원은 403 NOT_APPROVED. */
@Tag(name = "프로필 섹션")
@RestController
@RequestMapping("/me/awards")
@RequiredArgsConstructor
@AllowPendingMember
@RequireCompletedProfile
@ApiResponse(responseCode = "401", description = "토큰 없음·만료 (UNAUTHENTICATED)",
		content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
@ApiResponse(responseCode = "403", description = "반려된 회원 (NOT_APPROVED), 온보딩 전 (PROFILE_NOT_COMPLETED)",
		content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class AwardController {

	private final AwardService awardService;

	@Operation(summary = "수상 추가 (PROFILE-11)", description = "최대 10개.")
	@ApiResponse(responseCode = "201")
	@ApiResponse(responseCode = "400", description = "필수값 누락, 글자 수 초과, 형식 오류 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "최대 개수 초과 (LIMIT_EXCEEDED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping
	public ResponseEntity<AwardItem> create(
			@Parameter(hidden = true) @CurrentUser Long userId,
			@Valid @RequestBody AwardRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(awardService.create(userId, request));
	}

	@Operation(summary = "수상 수정 (PROFILE-11)", description = "보낸 값으로 통째로 바꾼다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "400", description = "필수값 누락, 글자 수 초과, 형식 오류 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "없는 항목이거나 남의 항목 (NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PutMapping("/{itemId}")
	public AwardItem update(
			@Parameter(hidden = true) @CurrentUser Long userId,
			@PathVariable Long itemId,
			@Valid @RequestBody AwardRequest request) {
		return awardService.update(userId, itemId, request);
	}

	@Operation(summary = "수상 삭제 (PROFILE-11)")
	@ApiResponse(responseCode = "204")
	@ApiResponse(responseCode = "404", description = "없는 항목이거나 남의 항목 (NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@DeleteMapping("/{itemId}")
	public ResponseEntity<Void> delete(
			@Parameter(hidden = true) @CurrentUser Long userId,
			@PathVariable Long itemId) {
		awardService.delete(userId, itemId);
		return ResponseEntity.noContent().build();
	}
}
