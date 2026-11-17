package com.lionlink.backend.domain.profile.section.controller;

import com.lionlink.backend.domain.profile.basic.dto.response.ProfileResponse.LionActivityItem;
import com.lionlink.backend.domain.profile.section.dto.request.LionActivityRequest;
import com.lionlink.backend.domain.profile.section.service.LionActivityService;
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

/** PROFILE-11 멋사 활동. 승인 대기 회원도 쓸 수 있고 반려 회원은 403 NOT_APPROVED. */
@Tag(name = "프로필 섹션")
@RestController
@RequestMapping("/me/lion-activities")
@RequiredArgsConstructor
@AllowPendingMember
@RequireCompletedProfile
@ApiResponse(responseCode = "401", description = "토큰 없음·만료 (UNAUTHENTICATED)",
		content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
@ApiResponse(responseCode = "403", description = "반려된 회원 (NOT_APPROVED), 온보딩 전 (PROFILE_NOT_COMPLETED)",
		content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class LionActivityController {

	private final LionActivityService lionActivityService;

	@Operation(summary = "멋사 활동 추가 (PROFILE-11)", description = "최대 6개. 같은 기수는 두 번 추가할 수 없다.")
	@ApiResponse(responseCode = "201")
	@ApiResponse(responseCode = "400", description = "필수값 누락, 기수 범위, 파트 개수·중복 오류 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "최대 개수 초과 (LIMIT_EXCEEDED), 같은 기수 (DUPLICATE_ITEM)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping
	public ResponseEntity<LionActivityItem> create(
			@Parameter(hidden = true) @CurrentUser Long userId,
			@Valid @RequestBody LionActivityRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(lionActivityService.create(userId, request));
	}

	@Operation(summary = "멋사 활동 수정 (PROFILE-11)",
			description = "보낸 값으로 통째로 바꾼다. 가입 기수 항목(isDefault)은 role·parts만 반영하고 다른 기수를 보내면 400.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "400", description = "필수값 누락, 파트 오류, 가입 기수 항목의 기수 변경 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "없는 항목이거나 남의 항목 (NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "같은 기수 (DUPLICATE_ITEM)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PutMapping("/{itemId}")
	public LionActivityItem update(
			@Parameter(hidden = true) @CurrentUser Long userId,
			@PathVariable Long itemId,
			@Valid @RequestBody LionActivityRequest request) {
		return lionActivityService.update(userId, itemId, request);
	}

	@Operation(summary = "멋사 활동 삭제 (PROFILE-11)", description = "가입 기수 항목(isDefault)은 지울 수 없다.")
	@ApiResponse(responseCode = "204")
	@ApiResponse(responseCode = "403", description = "가입 기수 항목 (DEFAULT_ITEM_NOT_DELETABLE)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "없는 항목이거나 남의 항목 (NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@DeleteMapping("/{itemId}")
	public ResponseEntity<Void> delete(
			@Parameter(hidden = true) @CurrentUser Long userId,
			@PathVariable Long itemId) {
		lionActivityService.delete(userId, itemId);
		return ResponseEntity.noContent().build();
	}
}
