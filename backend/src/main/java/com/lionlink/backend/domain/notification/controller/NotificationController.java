package com.lionlink.backend.domain.notification.controller;

import com.lionlink.backend.domain.notification.dto.request.NotificationTabFilter;
import com.lionlink.backend.domain.notification.dto.response.NotificationListResponse;
import com.lionlink.backend.domain.notification.service.NotificationService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "알림")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;

	@Operation(summary = "최근 알림 20개", description = "선택한 탭의 최근 알림 20개와 탭별 안 읽은 개수를 준다. 계정 알림(ACCOUNT)은 전체(ALL)에서만 보인다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "400", description = "지원하지 않는 탭 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "미인증 (UNAUTHENTICATED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "403", description = "승인 전 회원 (NOT_APPROVED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping
	public NotificationListResponse getNotifications(
			@CurrentUser Long userId,
			@Parameter(description = "탭 (ALL, COFFEE_CHAT, CHAT, COMMUNITY)")
			@RequestParam(defaultValue = "ALL") NotificationTabFilter tab
	) {
		return notificationService.getNotifications(userId, tab);
	}

	@Operation(summary = "알림 하나 읽음 처리", description = "이미 읽은 알림이어도 204를 준다.")
	@ApiResponse(responseCode = "204")
	@ApiResponse(responseCode = "401", description = "미인증 (UNAUTHENTICATED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "없거나 내 알림이 아님 (NOTIFICATION_NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PutMapping("/{notificationId}/read")
	public ResponseEntity<Void> read(
			@CurrentUser Long userId,
			@Parameter(description = "알림 ID") @PathVariable Long notificationId
	) {
		notificationService.read(userId, notificationId);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "알림 모두 읽음 처리", description = "탭과 상관없이 내 알림을 모두 읽음으로 바꾼다.")
	@ApiResponse(responseCode = "204")
	@ApiResponse(responseCode = "401", description = "미인증 (UNAUTHENTICATED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PutMapping("/read-all")
	public ResponseEntity<Void> readAll(@CurrentUser Long userId) {
		notificationService.readAll(userId);
		return ResponseEntity.noContent().build();
	}
}
