package com.lionlink.backend.domain.coffeechat.controller;

import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatAcceptRequest;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatBox;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatCancelRequest;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatCreateRequest;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatFilter;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatRejectRequest;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatScheduleRequest;
import com.lionlink.backend.domain.coffeechat.dto.response.CoffeeChatCreateResponse;
import com.lionlink.backend.domain.coffeechat.dto.response.CoffeeChatDetailResponse;
import com.lionlink.backend.domain.coffeechat.dto.response.CoffeeChatListResponse;
import com.lionlink.backend.domain.coffeechat.service.CoffeeChatQueryService;
import com.lionlink.backend.domain.coffeechat.service.CoffeeChatService;
import com.lionlink.backend.global.error.ErrorResponse;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "커피챗")
@RestController
@RequestMapping("/coffee-chats")
@RequiredArgsConstructor
public class CoffeeChatController {

	private final CoffeeChatService coffeeChatService;
	private final CoffeeChatQueryService coffeeChatQueryService;

	@Operation(summary = "커피챗 신청", description = "fromRecruitId가 있으면 모집글의 사자와 참여자로 신청하고 모집은 APPLIED가 된다.")
	@ApiResponse(responseCode = "201")
	@ApiResponse(responseCode = "400", description = "입력값 오류 (VALIDATION_ERROR, CANNOT_REQUEST_SELF, INVALID_TOPIC, INVALID_MEMBERS)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "사자 또는 모집글 없음 (USER_NOT_FOUND, RECRUIT_NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "신청할 수 없음 (NOT_ACCEPTING, GROUP_COFFEE_CHAT_NOT_ALLOWED, OPEN_COFFEE_CHAT_EXISTS, COOLDOWN, RECRUIT_CLOSED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping
	public ResponseEntity<CoffeeChatCreateResponse> create(@CurrentUser Long userId, @Valid @RequestBody CoffeeChatCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(coffeeChatService.create(userId, request));
	}

	@Operation(summary = "내 커피챗 목록", description = "받은 신청(RECEIVED)·보낸 신청(SENT)을 최근 신청순으로 반환한다.")
	@ApiResponse(responseCode = "200")
	@GetMapping
	public CoffeeChatListResponse getList(
			@CurrentUser Long userId,
			@Parameter(description = "RECEIVED 받은 신청, SENT 보낸 신청") @RequestParam(defaultValue = "RECEIVED") CoffeeChatBox box,
			@Parameter(description = "ALL, PENDING, ACTIVE(수락됨·일정 확정), ENDED") @RequestParam(defaultValue = "ALL") CoffeeChatFilter filter,
			@RequestParam(defaultValue = "0") @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.") int page,
			@RequestParam(defaultValue = "20")
			@Min(value = 1, message = "페이지 크기는 1~100이어야 합니다.")
			@Max(value = 100, message = "페이지 크기는 1~100이어야 합니다.") int size
	) {
		return coffeeChatQueryService.getList(userId, box, filter, PageRequest.of(page, size));
	}

	@Operation(summary = "커피챗 상세", description = "actions에 지금 내가 할 수 있는 동작이 담긴다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "404", description = "없거나 참여하지 않은 커피챗 (COFFEE_CHAT_NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/{coffeeChatId}")
	public CoffeeChatDetailResponse getDetail(@CurrentUser Long userId, @PathVariable Long coffeeChatId) {
		return coffeeChatQueryService.getDetail(userId, coffeeChatId);
	}

	@Operation(summary = "커피챗 수락", description = "받는 사자만. 수락하면 채팅방이 열린다.")
	@PostMapping("/{coffeeChatId}/accept")
	public CoffeeChatDetailResponse accept(@CurrentUser Long userId, @PathVariable Long coffeeChatId,
			@Valid @RequestBody CoffeeChatAcceptRequest request) {
		return coffeeChatService.accept(userId, coffeeChatId, request);
	}

	@Operation(summary = "커피챗 거절", description = "받는 사자만.")
	@PostMapping("/{coffeeChatId}/reject")
	public CoffeeChatDetailResponse reject(@CurrentUser Long userId, @PathVariable Long coffeeChatId,
			@Valid @RequestBody CoffeeChatRejectRequest request) {
		return coffeeChatService.reject(userId, coffeeChatId, request);
	}

	@Operation(summary = "신청 취소", description = "대기 중일 때 대표 신청자만.")
	@PostMapping("/{coffeeChatId}/withdraw")
	public CoffeeChatDetailResponse withdraw(@CurrentUser Long userId, @PathVariable Long coffeeChatId) {
		return coffeeChatService.withdraw(userId, coffeeChatId);
	}

	@Operation(summary = "일정 확정·변경", description = "받는 사자 또는 대표 신청자. 오프라인이면 장소가 필요하다.")
	@PutMapping("/{coffeeChatId}/schedule")
	public CoffeeChatDetailResponse schedule(@CurrentUser Long userId, @PathVariable Long coffeeChatId,
			@Valid @RequestBody CoffeeChatScheduleRequest request) {
		return coffeeChatService.schedule(userId, coffeeChatId, request);
	}

	@Operation(summary = "커피챗 취소", description = "수락 이후 받는 사자 또는 대표 신청자. 사유가 필요하다.")
	@PostMapping("/{coffeeChatId}/cancel")
	public CoffeeChatDetailResponse cancel(@CurrentUser Long userId, @PathVariable Long coffeeChatId,
			@Valid @RequestBody CoffeeChatCancelRequest request) {
		return coffeeChatService.cancel(userId, coffeeChatId, request);
	}

	@Operation(summary = "만남 완료", description = "일정 확정 후 만남 시각이 지나면 받는 사자 또는 대표 신청자가 완료한다.")
	@PostMapping("/{coffeeChatId}/complete")
	public CoffeeChatDetailResponse complete(@CurrentUser Long userId, @PathVariable Long coffeeChatId) {
		return coffeeChatService.complete(userId, coffeeChatId);
	}

	@Operation(summary = "함께 신청자 빠지기", description = "진행 중인 여러 명 커피챗에서 함께 신청자가 빠진다. 단체 채팅방에서도 나간다.")
	@ApiResponse(responseCode = "204")
	@DeleteMapping("/{coffeeChatId}/members/me")
	public ResponseEntity<Void> leave(@CurrentUser Long userId, @PathVariable Long coffeeChatId) {
		coffeeChatService.leave(userId, coffeeChatId);
		return ResponseEntity.noContent().build();
	}
}
