package com.lionlink.backend.domain.admin.controller;

import com.lionlink.backend.domain.admin.dto.request.AdminSignupInfoRequest;
import com.lionlink.backend.domain.admin.dto.request.ApproveBatchRequest;
import com.lionlink.backend.domain.admin.dto.request.RejectRequest;
import com.lionlink.backend.domain.admin.dto.response.AdminSignupInfoResponse;
import com.lionlink.backend.domain.admin.dto.response.ApproveBatchResponse;
import com.lionlink.backend.domain.admin.dto.response.ApproveResponse;
import com.lionlink.backend.domain.admin.dto.response.PendingCountResponse;
import com.lionlink.backend.domain.admin.dto.response.RejectResponse;
import com.lionlink.backend.domain.admin.dto.response.SignupListItemResponse;
import com.lionlink.backend.domain.admin.service.AdminSignupService;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.global.error.ErrorResponse;
import com.lionlink.backend.global.response.PageResponse;
import com.lionlink.backend.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

	@Operation(summary = "가입 승인 (ADMIN-03)",
			description = "승인 대기·반려 회원을 승인한다(반려됨 탭의 '승인으로 바꾸기' 포함). 반려 사유는 비우고, "
					+ "본인에게 서비스 안 알림을 만든다. 동시에 처리하면 먼저 처리한 쪽만 반영된다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "403", description = "관리자가 아님 (FORBIDDEN)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "없는 회원이거나 탈퇴한 회원 (NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "이미 승인됨 (ALREADY_PROCESSED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/{userId}/approve")
	public ApproveResponse approve(
			@Parameter(hidden = true) @CurrentUser Long adminId,
			@PathVariable Long userId
	) {
		return adminSignupService.approve(adminId, userId);
	}

	@Operation(summary = "가입 반려 (ADMIN-04)",
			description = "승인 대기 회원만 반려할 수 있다. 알림·메일은 보내지 않는다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "400", description = "ETC인데 reasonText가 5자 미만·100자 초과, 없는 reasonType (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "403", description = "관리자가 아님 (FORBIDDEN)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "없는 회원이거나 탈퇴한 회원 (NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "승인 대기가 아님 (ALREADY_PROCESSED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/{userId}/reject")
	public RejectResponse reject(
			@Parameter(hidden = true) @CurrentUser Long adminId,
			@PathVariable Long userId,
			@Valid @RequestBody RejectRequest request
	) {
		return adminSignupService.reject(adminId, userId, request);
	}

	@Operation(summary = "선택 승인 (ADMIN-05)",
			description = "한 명씩 ADMIN-03과 같은 규칙으로 처리한다. 일부가 실패해도 나머지는 승인되고, "
					+ "성공(succeeded)과 실패(failed)를 나눠서 돌려준다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "400", description = "userIds가 비었거나 50개 초과, 중복 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "403", description = "관리자가 아님 (FORBIDDEN)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/approve-batch")
	public ApproveBatchResponse approveBatch(
			@Parameter(hidden = true) @CurrentUser Long adminId,
			@Valid @RequestBody ApproveBatchRequest request
	) {
		return adminSignupService.approveBatch(adminId, request.userIds());
	}

	@Operation(summary = "이름·기수 수정 (ADMIN-06)",
			description = "상태와 상관없이 고친다. 기수가 바뀌면 자동 생성된 멋사 활동 항목의 기수도 같이 바꾼다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "400", description = "이름 길이·기수 범위 오류 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "403", description = "관리자가 아님 (FORBIDDEN)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "없는 회원이거나 탈퇴한 회원 (NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "바뀔 기수에 이미 다른 멋사 활동이 있음 (DUPLICATE_ITEM)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PatchMapping("/{userId}/signup-info")
	public AdminSignupInfoResponse updateSignupInfo(
			@PathVariable Long userId,
			@Valid @RequestBody AdminSignupInfoRequest request
	) {
		return adminSignupService.updateSignupInfo(userId, request);
	}
}
