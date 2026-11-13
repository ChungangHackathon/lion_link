package com.lionlink.backend.domain.profile.coffee.controller;

import com.lionlink.backend.domain.profile.coffee.dto.request.CoffeePeriodRequest;
import com.lionlink.backend.domain.profile.coffee.dto.response.CoffeePeriodResponse;
import com.lionlink.backend.domain.profile.coffee.service.CoffeePeriodService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** PROFILE-15 커피챗 기간 추가·삭제. 승인 대기 회원도 쓸 수 있고 반려 회원은 403 NOT_APPROVED. */
@Tag(name = "커피챗 받기 설정")
@RestController
@RequestMapping("/me/coffee-periods")
@RequiredArgsConstructor
@AllowPendingMember
@RequireCompletedProfile
@ApiResponse(responseCode = "401", description = "토큰 없음·만료 (UNAUTHENTICATED)",
		content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
@ApiResponse(responseCode = "403", description = "반려된 회원 (NOT_APPROVED), 온보딩 전 (PROFILE_NOT_COMPLETED)",
		content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class CoffeePeriodController {

	private final CoffeePeriodService coffeePeriodService;

	@Operation(summary = "기간 추가 (PROFILE-15)",
			description = "기본 상태가 ON이면 쉬는 기간, OFF면 받는 기간. 최대 5개. mergeOverlap이 true면 겹치는 기간을 하나로 합쳐 저장한다.")
	@ApiResponse(responseCode = "201")
	@ApiResponse(responseCode = "400", description = "종료일이 시작일보다 빠름, 시작일이 과거·1년 초과, 180일 초과 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "이미 등록한 기간과 겹침 (PERIOD_OVERLAP), 이미 5개 (PERIOD_LIMIT)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping
	public ResponseEntity<CoffeePeriodResponse> add(
			@Parameter(hidden = true) @CurrentUser Long userId,
			@Valid @RequestBody CoffeePeriodRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(coffeePeriodService.add(userId, request));
	}

	@Operation(summary = "기간 삭제 (PROFILE-15)")
	@ApiResponse(responseCode = "204")
	@ApiResponse(responseCode = "404", description = "없는 기간이거나 남의 기간 (NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@DeleteMapping("/{periodId}")
	public ResponseEntity<Void> delete(
			@Parameter(hidden = true) @CurrentUser Long userId,
			@PathVariable Long periodId) {
		coffeePeriodService.delete(userId, periodId);
		return ResponseEntity.noContent().build();
	}
}
