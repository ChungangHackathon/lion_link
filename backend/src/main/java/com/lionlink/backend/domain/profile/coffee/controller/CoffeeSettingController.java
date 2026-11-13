package com.lionlink.backend.domain.profile.coffee.controller;

import com.lionlink.backend.domain.profile.coffee.dto.request.CoffeeSettingUpdateRequest;
import com.lionlink.backend.domain.profile.coffee.dto.request.CoffeeSwitchRequest;
import com.lionlink.backend.domain.profile.coffee.dto.response.CoffeeSettingResponse;
import com.lionlink.backend.domain.profile.coffee.service.CoffeeSettingService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** PROFILE-12~14 커피챗 받기 설정. 승인 대기 회원도 쓸 수 있고 반려 회원은 403 NOT_APPROVED. */
@Tag(name = "커피챗 받기 설정")
@RestController
@RequestMapping("/me/coffee-settings")
@RequiredArgsConstructor
@AllowPendingMember
@RequireCompletedProfile
@ApiResponse(responseCode = "401", description = "토큰 없음·만료 (UNAUTHENTICATED)",
		content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
@ApiResponse(responseCode = "403", description = "반려된 회원 (NOT_APPROVED), 온보딩 전 (PROFILE_NOT_COMPLETED)",
		content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class CoffeeSettingController {

	private final CoffeeSettingService coffeeSettingService;

	@Operation(summary = "커피챗 받기 설정 조회 (PROFILE-12)",
			description = "지난 기간은 뺀다. status는 기본 상태와 오늘이 기간에 들어 있는지를 합쳐서 서버가 정한다.")
	@ApiResponse(responseCode = "200")
	@GetMapping
	public CoffeeSettingResponse get(@Parameter(hidden = true) @CurrentUser Long userId) {
		return coffeeSettingService.get(userId);
	}

	@Operation(summary = "커피챗 받기 설정 저장 (PROFILE-13)",
			description = "보낸 값으로 통째로 바꾼다. 기본 상태를 바꾸면 등록된 기간이 모두 지워지므로 기간이 있으면 confirmClearPeriods가 true여야 한다.")
	@ApiResponse(responseCode = "200", description = "PROFILE-12 형식")
	@ApiResponse(responseCode = "400", description = "방식이 비었음, 없는 열거값, 요일 중복, 글자 수 초과 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "기간이 있는데 확인 없이 기본 상태를 바꿈 (PERIODS_WILL_BE_CLEARED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PutMapping
	public CoffeeSettingResponse update(
			@Parameter(hidden = true) @CurrentUser Long userId,
			@Valid @RequestBody CoffeeSettingUpdateRequest request) {
		return coffeeSettingService.update(userId, request);
	}

	@Operation(summary = "홈 스위치 받기·쉬기 (PROFILE-14)",
			description = "지금 상태에 따라 서버가 처리한다. 이미 원하는 상태면 아무것도 바꾸지 않고 200.")
	@ApiResponse(responseCode = "200", description = "PROFILE-12 형식")
	@ApiResponse(responseCode = "400", description = "끌 때 restFor 누락, CUSTOM인데 날짜 누락·범위 오류 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "기간이 이미 5개 (PERIOD_LIMIT), 기본 상태가 바뀌는데 기간이 있고 확인 없음 (PERIODS_WILL_BE_CLEARED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/switch")
	public CoffeeSettingResponse switchReceiving(
			@Parameter(hidden = true) @CurrentUser Long userId,
			@Valid @RequestBody CoffeeSwitchRequest request) {
		return coffeeSettingService.switchReceiving(userId, request);
	}
}
