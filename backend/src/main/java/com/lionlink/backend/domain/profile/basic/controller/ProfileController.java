package com.lionlink.backend.domain.profile.basic.controller;

import com.lionlink.backend.domain.auth.dto.response.MeResponse;
import com.lionlink.backend.domain.profile.basic.dto.request.OnboardingRequest;
import com.lionlink.backend.domain.profile.basic.dto.response.ProfileResponse;
import com.lionlink.backend.domain.profile.basic.service.MyProfileService;
import com.lionlink.backend.domain.profile.basic.service.ProfileOnboardingService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 승인 대기 회원도 쓰는 프로필 API. 반려 회원은 403 NOT_APPROVED. */
@Tag(name = "프로필")
@RestController
@RequestMapping("/me/profile")
@RequiredArgsConstructor
@AllowPendingMember
public class ProfileController {

	private final ProfileOnboardingService profileOnboardingService;
	private final MyProfileService myProfileService;

	@Operation(summary = "기본 정보 입력(온보딩) (PROFILE-01)",
			description = "가입 직후 처음 1번만. 프로필과 경력·학력·멋사 활동·외부 링크·커피챗·알림 설정 기본값을 함께 만든다. "
					+ "승인 대기·승인 회원이 쓸 수 있다.")
	@ApiResponse(responseCode = "200", description = "AUTH-05 형식 (profileCompleted: true)")
	@ApiResponse(responseCode = "400", description = "필수값 누락, 파트 3개 이상, 중복 기수, URL 형식 오류 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "토큰 없음·만료 (UNAUTHENTICATED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "403", description = "반려된 회원 (NOT_APPROVED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "이미 기본 정보를 입력함 (ALREADY_COMPLETED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/onboarding")
	public MeResponse onboard(
			@Parameter(hidden = true) @CurrentUser Long userId,
			@Valid @RequestBody OnboardingRequest request) {
		return profileOnboardingService.onboard(userId, request);
	}

	@Operation(summary = "내 프로필 조회 (PROFILE-02)",
			description = "공개 범위를 적용하지 않은 전체 내용과 현재 공개 범위 설정. 목록형 섹션은 최신순, 진행 중인 항목이 맨 위.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "401", description = "토큰 없음·만료 (UNAUTHENTICATED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "403", description = "반려된 회원 (NOT_APPROVED), 온보딩 전 (PROFILE_NOT_COMPLETED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@RequireCompletedProfile
	@GetMapping
	public ProfileResponse getMyProfile(@Parameter(hidden = true) @CurrentUser Long userId) {
		return myProfileService.getMyProfile(userId);
	}
}
