package com.lionlink.backend.domain.profile.basic.controller;

import com.lionlink.backend.domain.auth.dto.response.MeResponse;
import com.lionlink.backend.domain.profile.basic.dto.request.BasicInfoUpdateRequest;
import com.lionlink.backend.domain.profile.basic.dto.request.BioUpdateRequest;
import com.lionlink.backend.domain.profile.basic.dto.request.ContactUpdateRequest;
import com.lionlink.backend.domain.profile.basic.dto.request.LinksUpdateRequest;
import com.lionlink.backend.domain.profile.basic.dto.request.OnboardingRequest;
import com.lionlink.backend.domain.profile.basic.dto.request.SkillsUpdateRequest;
import com.lionlink.backend.domain.profile.basic.dto.request.VisibilityUpdateRequest;
import com.lionlink.backend.domain.profile.basic.dto.response.BasicInfoResponse;
import com.lionlink.backend.domain.profile.basic.dto.response.BioResponse;
import com.lionlink.backend.domain.profile.basic.dto.response.ContactResponse;
import com.lionlink.backend.domain.profile.basic.dto.response.LinksResponse;
import com.lionlink.backend.domain.profile.basic.dto.response.ProfileResponse;
import com.lionlink.backend.domain.profile.basic.dto.response.SkillsResponse;
import com.lionlink.backend.domain.profile.basic.dto.response.VisibilityResponse;
import com.lionlink.backend.domain.profile.basic.service.MyProfileService;
import com.lionlink.backend.domain.profile.basic.service.ProfileEditService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
	private final ProfileEditService profileEditService;

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

	@Operation(summary = "기본 정보 수정 (PROFILE-04)",
			description = "보낸 값으로 통째로 바꾼다. 이름·기수는 받지 않는다. 현재 상태를 바꿔도 경력은 그대로 둔다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "400", description = "글자 수 초과, 파트·관심 분야 개수·중복 오류, 없는 열거값 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "토큰 없음·만료 (UNAUTHENTICATED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "403", description = "반려된 회원 (NOT_APPROVED), 온보딩 전 (PROFILE_NOT_COMPLETED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@RequireCompletedProfile
	@PutMapping("/basic")
	public BasicInfoResponse updateBasic(
			@Parameter(hidden = true) @CurrentUser Long userId,
			@Valid @RequestBody BasicInfoUpdateRequest request) {
		return profileEditService.updateBasic(userId, request);
	}

	@Operation(summary = "소개 수정 (PROFILE-06)",
			description = "보낸 값으로 통째로 바꾼다. null이나 빈 값이면 소개가 비워진다. 줄바꿈은 유지한다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "400", description = "1000자 초과 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "토큰 없음·만료 (UNAUTHENTICATED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "403", description = "반려된 회원 (NOT_APPROVED), 온보딩 전 (PROFILE_NOT_COMPLETED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@RequireCompletedProfile
	@PutMapping("/bio")
	public BioResponse updateBio(
			@Parameter(hidden = true) @CurrentUser Long userId,
			@Valid @RequestBody BioUpdateRequest request) {
		return profileEditService.updateBio(userId, request);
	}

	@Operation(summary = "대표 기술 수정 (PROFILE-07)",
			description = "보낸 값으로 통째로 바꾼다. 같은 기술은 앞쪽만 남기고 순서는 유지한다. 비우면 섹션이 사라진다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "400", description = "6개 이상, 태그가 비었거나 20자 초과 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "토큰 없음·만료 (UNAUTHENTICATED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "403", description = "반려된 회원 (NOT_APPROVED), 온보딩 전 (PROFILE_NOT_COMPLETED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@RequireCompletedProfile
	@PutMapping("/skills")
	public SkillsResponse updateSkills(
			@Parameter(hidden = true) @CurrentUser Long userId,
			@Valid @RequestBody SkillsUpdateRequest request) {
		return profileEditService.updateSkills(userId, request);
	}

	@Operation(summary = "외부 링크 수정 (PROFILE-08)",
			description = "보낸 값으로 통째로 바꾼다. 보낸 순서를 유지한다. 비우면 섹션이 사라진다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "400", description = "6개 이상, 없는 링크 종류, http(s)가 아닌 주소 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "토큰 없음·만료 (UNAUTHENTICATED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "403", description = "반려된 회원 (NOT_APPROVED), 온보딩 전 (PROFILE_NOT_COMPLETED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@RequireCompletedProfile
	@PutMapping("/links")
	public LinksResponse updateLinks(
			@Parameter(hidden = true) @CurrentUser Long userId,
			@Valid @RequestBody LinksUpdateRequest request) {
		return profileEditService.updateLinks(userId, request);
	}

	@Operation(summary = "연락처 수정 (PROFILE-09)",
			description = "보낸 값으로 통째로 바꾼다. 이메일은 다른 동문에게 보여주는 용도이며 가입 이메일은 바뀌지 않는다. "
					+ "전화번호는 비우면 삭제된다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "400", description = "이메일 없음·형식 오류, 전화번호 형식 오류 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "토큰 없음·만료 (UNAUTHENTICATED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "403", description = "반려된 회원 (NOT_APPROVED), 온보딩 전 (PROFILE_NOT_COMPLETED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@RequireCompletedProfile
	@PutMapping("/contact")
	public ContactResponse updateContact(
			@Parameter(hidden = true) @CurrentUser Long userId,
			@Valid @RequestBody ContactUpdateRequest request) {
		return profileEditService.updateContact(userId, request);
	}

	@Operation(summary = "공개 범위 수정 (PROFILE-10)",
			description = "보낸 키만 바꾸고 안 보낸 키는 그대로 둔다. 응답은 네 값 모두. 회사명(company)은 CONNECTED를 쓸 수 없다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "400", description = "없는 값, company에 CONNECTED (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "토큰 없음·만료 (UNAUTHENTICATED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "403", description = "반려된 회원 (NOT_APPROVED), 온보딩 전 (PROFILE_NOT_COMPLETED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@RequireCompletedProfile
	@PatchMapping("/visibility")
	public VisibilityResponse updateVisibility(
			@Parameter(hidden = true) @CurrentUser Long userId,
			@Valid @RequestBody VisibilityUpdateRequest request) {
		return profileEditService.updateVisibility(userId, request);
	}
}
