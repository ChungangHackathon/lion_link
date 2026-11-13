package com.lionlink.backend.domain.notification.controller;

import com.lionlink.backend.domain.notification.dto.request.NotificationSettingUpdateRequest;
import com.lionlink.backend.domain.notification.dto.response.NotificationSettingResponse;
import com.lionlink.backend.domain.notification.entity.EmailType;
import com.lionlink.backend.domain.notification.service.NotificationSettingService;
import com.lionlink.backend.global.error.ErrorResponse;
import com.lionlink.backend.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "알림 설정")
@RestController
@RequiredArgsConstructor
public class NotificationSettingController {

	private final NotificationSettingService notificationSettingService;

	@Operation(summary = "이메일 알림 설정 조회", description = "설정이 아직 없으면 모두 켜진 기본값으로 만들어 준다. 승인 대기 회원도 쓸 수 있다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "401", description = "미인증 (UNAUTHENTICATED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "403", description = "반려된 회원 (NOT_APPROVED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/me/notification-settings")
	public NotificationSettingResponse getSettings(@CurrentUser Long userId) {
		return notificationSettingService.getSettings(userId);
	}

	@Operation(summary = "이메일 알림 종류별 켜기 끄기", description = "보낸 항목만 바꾸고 바뀐 설정 전체를 준다. 승인 대기 회원도 쓸 수 있다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "400", description = "본문 형식 오류 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "미인증 (UNAUTHENTICATED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "403", description = "반려된 회원 (NOT_APPROVED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PatchMapping("/me/notification-settings")
	public NotificationSettingResponse updateSettings(
			@CurrentUser Long userId,
			@RequestBody NotificationSettingUpdateRequest request
	) {
		return notificationSettingService.updateSettings(userId, request);
	}

	@Operation(summary = "메일 링크로 수신 거부", description = "로그인 없이 메일에 담긴 수신 거부 토큰으로 해당 종류의 메일을 끈다. 이미 꺼져 있어도 204를 준다.")
	@ApiResponse(responseCode = "204")
	@ApiResponse(responseCode = "400", description = "잘못된 토큰 (INVALID_UNSUBSCRIBE_TOKEN), 토큰이나 종류가 없거나 지원하지 않는 종류 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/email-notifications/unsubscribe")
	public ResponseEntity<Void> unsubscribe(
			@Parameter(description = "수신 거부 토큰") @RequestParam String token,
			@Parameter(description = "메일 종류 (COFFEE_CHAT_REQUEST, MESSAGE_REQUEST, SIGNUP_APPROVED)") @RequestParam EmailType type
	) {
		notificationSettingService.unsubscribe(token, type);
		return ResponseEntity.noContent().build();
	}
}
