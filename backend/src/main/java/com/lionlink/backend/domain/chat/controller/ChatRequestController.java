package com.lionlink.backend.domain.chat.controller;

import com.lionlink.backend.domain.chat.dto.request.ChatRequestCreateRequest;
import com.lionlink.backend.domain.chat.dto.request.ChatRequestDeclineRequest;
import com.lionlink.backend.domain.chat.dto.request.ChatRequestMessageUpdateRequest;
import com.lionlink.backend.domain.chat.dto.response.ChatMessageResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatRequestCreateResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatRequestDetailResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatRequestSummaryResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatRoomDetailResponse;
import com.lionlink.backend.domain.chat.service.ChatRequestService;
import com.lionlink.backend.global.response.SliceResponse;
import com.lionlink.backend.global.error.ErrorResponse;
import com.lionlink.backend.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "메시지 요청")
@RestController
@RequestMapping("/chat-requests")
@RequiredArgsConstructor
public class ChatRequestController {

	private final ChatRequestService chatRequestService;

	@Operation(summary = "메시지 요청 보내기", description = "이미 대화 중이거나 요청 중이면 새로 만들지 않고 그 방을 200으로 돌려준다(created=false).")
	@ApiResponse(responseCode = "201")
	@ApiResponse(responseCode = "400", description = "입력값 오류 (VALIDATION_ERROR, CANNOT_CHAT_SELF)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "받는 회원 없음 (USER_NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "거절된 지 7일 안 (REQUEST_COOLDOWN)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping
	public ResponseEntity<ChatRequestCreateResponse> create(@CurrentUser Long userId, @Valid @RequestBody ChatRequestCreateRequest request) {
		ChatRequestCreateResponse response = chatRequestService.create(userId, request);
		return ResponseEntity.status(response.created() ? HttpStatus.CREATED : HttpStatus.OK).body(response);
	}

	@Operation(summary = "받은 요청함", description = "나에게 온 대기 중 요청을 최근 순서로 반환한다.")
	@GetMapping
	public SliceResponse<ChatRequestSummaryResponse> getReceived(
			@CurrentUser Long userId,
			@RequestParam(defaultValue = "0") @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.") int page,
			@RequestParam(defaultValue = "20")
			@Min(value = 1, message = "페이지 크기는 1~100이어야 합니다.")
			@Max(value = 100, message = "페이지 크기는 1~100이어야 합니다.") int size
	) {
		return chatRequestService.getReceived(userId, PageRequest.of(page, size));
	}

	@Operation(summary = "요청 보기")
	@GetMapping("/{roomId}")
	public ChatRequestDetailResponse getDetail(@CurrentUser Long userId, @PathVariable Long roomId) {
		return chatRequestService.getDetail(userId, roomId);
	}

	@Operation(summary = "요청 수락", description = "받는 사람만. 대화가 시작된다.")
	@PostMapping("/{roomId}/accept")
	public ChatRoomDetailResponse accept(@CurrentUser Long userId, @PathVariable Long roomId) {
		return chatRequestService.accept(userId, roomId);
	}

	@Operation(summary = "요청 거절", description = "받는 사람만. 기타는 사유가 필요하다.")
	@ApiResponse(responseCode = "204")
	@PostMapping("/{roomId}/decline")
	public ResponseEntity<Void> decline(@CurrentUser Long userId, @PathVariable Long roomId,
			@Valid @RequestBody ChatRequestDeclineRequest request) {
		chatRequestService.decline(userId, roomId, request);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "수락 전 첫 메시지 수정", description = "보낸 사람만.")
	@PatchMapping("/{roomId}/messages/{messageId}")
	public ChatMessageResponse updateMessage(@CurrentUser Long userId, @PathVariable Long roomId, @PathVariable Long messageId,
			@Valid @RequestBody ChatRequestMessageUpdateRequest request) {
		return chatRequestService.updateMessage(userId, roomId, messageId, request);
	}

	@Operation(summary = "수락 전 요청 취소", description = "보낸 사람만. 첫 메시지를 지우고 요청을 취소한다.")
	@ApiResponse(responseCode = "204")
	@DeleteMapping("/{roomId}/messages/{messageId}")
	public ResponseEntity<Void> cancel(@CurrentUser Long userId, @PathVariable Long roomId, @PathVariable Long messageId) {
		chatRequestService.cancel(userId, roomId, messageId);
		return ResponseEntity.noContent().build();
	}
}
