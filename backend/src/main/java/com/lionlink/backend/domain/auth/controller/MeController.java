package com.lionlink.backend.domain.auth.controller;

import com.lionlink.backend.domain.auth.dto.request.SignupInfoRequest;
import com.lionlink.backend.domain.auth.dto.response.MeResponse;
import com.lionlink.backend.domain.auth.service.MeService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 승인 대기·반려 회원도 부르는 API라 WebConfig 승인 검사 제외 경로에 들어 있다. */
@Tag(name = "계정·인증")
@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {

	private final MeService meService;

	@Operation(summary = "내 정보 조회 (AUTH-05)",
			description = "로그인한 모든 상태(승인 대기·반려 포함)에서 부른다. 프론트는 status와 profileCompleted로 화면을 나눈다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "401", description = "토큰 없음·만료, 탈퇴한 회원 (UNAUTHENTICATED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping
	public MeResponse getMe(@Parameter(hidden = true) @CurrentUser Long userId) {
		return meService.getMe(userId);
	}

	@Operation(summary = "가입 정보 수정 (AUTH-06)",
			description = "승인 대기(PENDING) 회원만 이름·기수를 고친다. 기수를 바꾸면 자동 생성된 멋사 활동 항목의 기수도 같이 바뀐다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "400", description = "글자 수·범위 오류 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "토큰 없음·만료, 탈퇴한 회원 (UNAUTHENTICATED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "반려된 회원 (NOT_PENDING), 이미 승인됨 (ALREADY_APPROVED), "
			+ "바뀔 기수에 이미 멋사 활동이 있음 (DUPLICATE_ITEM)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PatchMapping("/signup-info")
	public MeResponse updateSignupInfo(
			@Parameter(hidden = true) @CurrentUser Long userId,
			@Valid @RequestBody SignupInfoRequest request) {
		return meService.updateSignupInfo(userId, request);
	}

	@Operation(summary = "다시 신청 (AUTH-07)",
			description = "반려(REJECTED) 회원이 이름·기수를 고쳐 다시 신청한다. 상태가 PENDING으로 돌아가고 관리자에게 알림이 간다. "
					+ "이전 반려 사유는 남긴다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "400", description = "글자 수·범위 오류 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "토큰 없음·만료, 탈퇴한 회원 (UNAUTHENTICATED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "반려 상태가 아님 (NOT_REJECTED), "
			+ "바뀔 기수에 이미 멋사 활동이 있음 (DUPLICATE_ITEM)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/reapply")
	public MeResponse reapply(
			@Parameter(hidden = true) @CurrentUser Long userId,
			@Valid @RequestBody SignupInfoRequest request) {
		return meService.reapply(userId, request);
	}
}
