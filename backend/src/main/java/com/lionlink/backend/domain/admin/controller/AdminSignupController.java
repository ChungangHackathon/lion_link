package com.lionlink.backend.domain.admin.controller;

import com.lionlink.backend.domain.admin.dto.response.PendingCountResponse;
import com.lionlink.backend.domain.admin.dto.response.SignupListItemResponse;
import com.lionlink.backend.domain.admin.service.AdminSignupService;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.global.error.ErrorResponse;
import com.lionlink.backend.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 - 가입 승인")
@RestController
@RequestMapping("/admin/signups")
@RequiredArgsConstructor
public class AdminSignupController {

	private final AdminSignupService adminSignupService;

	@Operation(summary = "가입 목록 조회 (ADMIN-01)",
			description = "탭(승인 대기/승인됨/반려됨) 하나를 불러온다. 탈퇴한 회원은 나오지 않는다. "
					+ "승인 대기는 가입이 오래된 순, 승인됨·반려됨은 처리 시각이 최신인 순.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "400", description = "없는 status, size 범위 초과 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "403", description = "관리자가 아님 (FORBIDDEN)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping
	public PageResponse<SignupListItemResponse> getSignups(
			@Parameter(description = "PENDING 승인 대기(기본), APPROVED 승인됨, REJECTED 반려됨")
			@RequestParam(defaultValue = "PENDING") UserStatus status,
			@Parameter(description = "페이지 번호 (0부터)")
			@RequestParam(defaultValue = "0")
			@Min(value = 0, message = "페이지 번호는 0 이상이어야 해요.") int page,
			@Parameter(description = "페이지 크기 (1~100)")
			@RequestParam(defaultValue = "20")
			@Min(value = 1, message = "페이지 크기는 1~100이어야 해요.")
			@Max(value = 100, message = "페이지 크기는 1~100이어야 해요.") int size
	) {
		return adminSignupService.getSignups(status, page, size);
	}

	@Operation(summary = "승인 대기 건수 (ADMIN-02)", description = "탈퇴한 회원은 세지 않는다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "403", description = "관리자가 아님 (FORBIDDEN)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/pending-count")
	public PendingCountResponse getPendingCount() {
		return adminSignupService.getPendingCount();
	}
}
