package com.lionlink.backend.domain.chat.controller;

import com.lionlink.backend.domain.chat.dto.request.ChatMessageSendRequest;
import com.lionlink.backend.domain.chat.dto.request.ChatPreferenceUpdateRequest;
import com.lionlink.backend.domain.chat.dto.request.ChatReadRequest;
import com.lionlink.backend.domain.chat.dto.response.ChatMessageListResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatMessageResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatPreferenceResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatReadResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatRoomDetailResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatUnreadCountResponse;
import com.lionlink.backend.domain.chat.service.ChatRoomService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "채팅방")
@RestController
@RequestMapping("/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {

	private final ChatRoomService chatRoomService;

	@Operation(summary = "내 채팅방 목록", description = "상단 고정한 방 먼저, 그다음 최근 대화순. 내가 보낸 요청(대기 중·거절됨)도 포함한다.")
	@GetMapping
	public SliceResponse<ChatRoomSummaryResponse> getList(
			@CurrentUser Long userId,
			@RequestParam(defaultValue = "0") @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.") int page,
			@RequestParam(defaultValue = "20")
			@Min(value = 1, message = "페이지 크기는 1~100이어야 합니다.")
			@Max(value = 100, message = "페이지 크기는 1~100이어야 합니다.") int size
	) {
		return chatRoomService.getList(userId, PageRequest.of(page, size));
	}

	@Operation(summary = "안 읽은 메시지 수", description = "상단 메뉴 채팅 배지용. 나가지 않은 대화 중인 방의 안 읽은 메시지 수와 받은 메시지 요청 수.")
	@GetMapping("/unread-count")
	public ChatUnreadCountResponse getUnreadCount(@CurrentUser Long userId) {
		return chatRoomService.getUnreadCount(userId);
	}

	@Operation(summary = "채팅방 정보")
	@ApiResponse(responseCode = "404", description = "없거나 참여하지 않은 방 (ROOM_NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/{roomId}")
	public ChatRoomDetailResponse getDetail(@CurrentUser Long userId, @PathVariable Long roomId) {
		return chatRoomService.getDetail(userId, roomId);
	}

	@Operation(summary = "메시지 목록", description = "before는 더 오래된 메시지, after는 그 다음 메시지. 항상 오래된 것부터 정렬한다.")
	@GetMapping("/{roomId}/messages")
	public ChatMessageListResponse getMessages(
			@CurrentUser Long userId,
			@PathVariable Long roomId,
			@RequestParam(required = false) Long before,
			@RequestParam(required = false) Long after,
			@RequestParam(defaultValue = "30")
			@Min(value = 1, message = "size는 1~100이어야 합니다.")
			@Max(value = 100, message = "size는 1~100이어야 합니다.") int size
	) {
		return chatRoomService.getMessages(userId, roomId, before, after, size);
	}

	@Operation(summary = "메시지 보내기", description = "같은 clientMsgId로 다시 보내면 처음 메시지를 돌려준다.")
	@ApiResponse(responseCode = "201")
	@ApiResponse(responseCode = "409", description = "대화 중이 아닌 방 (INVALID_ROOM_STATUS)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/{roomId}/messages")
	public ResponseEntity<ChatMessageResponse> send(@CurrentUser Long userId, @PathVariable Long roomId,
			@Valid @RequestBody ChatMessageSendRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(chatRoomService.send(userId, roomId, request));
	}

	@Operation(summary = "읽음 처리", description = "lastMessageId를 비우면 방의 최신 메시지까지 읽음으로 기록한다.")
	@PutMapping("/{roomId}/read")
	public ChatReadResponse read(@CurrentUser Long userId, @PathVariable Long roomId,
			@RequestBody(required = false) ChatReadRequest request) {
		return chatRoomService.read(userId, roomId, request);
	}

	@Operation(summary = "상단 고정, 알림 끄기", description = "보낸 값만 바꾼다.")
	@PatchMapping("/{roomId}/preferences")
	public ChatPreferenceResponse updatePreferences(@CurrentUser Long userId, @PathVariable Long roomId,
			@RequestBody ChatPreferenceUpdateRequest request) {
		return chatRoomService.updatePreferences(userId, roomId, request);
	}

	@Operation(summary = "나가기, 끝난 요청 숨기기", description = "1:1 방은 내 목록에서 숨기고, 단체방은 나간다.")
	@ApiResponse(responseCode = "204")
	@DeleteMapping("/{roomId}/members/me")
	public ResponseEntity<Void> leave(@CurrentUser Long userId, @PathVariable Long roomId) {
		chatRoomService.leave(userId, roomId);
		return ResponseEntity.noContent().build();
	}
}
