package com.lionlink.backend.domain.community.comment.controller;

import com.lionlink.backend.domain.community.comment.dto.request.CommentCreateRequest;
import com.lionlink.backend.domain.community.comment.dto.response.CommentResponse;
import com.lionlink.backend.domain.community.comment.service.CommentService;
import com.lionlink.backend.global.error.ErrorResponse;
import com.lionlink.backend.global.response.SliceResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "커뮤니티 댓글")
@RestController
@RequestMapping("/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

	private final CommentService commentService;

	@Operation(summary = "댓글 목록 조회",
			description = "오래된 순으로 반환한다. 삭제된 댓글은 deleted=true, 익명 댓글은 author=null로 내려간다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "404", description = "게시글 없음 (POST_NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping
	public SliceResponse<CommentResponse> getComments(
			@CurrentUser Long userId,
			@Parameter(description = "게시글 ID") @PathVariable Long postId,
			@Parameter(description = "페이지 번호 (0부터)")
			@RequestParam(defaultValue = "0")
			@Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.") int page,
			@Parameter(description = "페이지 크기 (1~100)")
			@RequestParam(defaultValue = "20")
			@Min(value = 1, message = "페이지 크기는 1~100이어야 합니다.")
			@Max(value = 100, message = "페이지 크기는 1~100이어야 합니다.") int size
	) {
		return commentService.getComments(userId, postId, PageRequest.of(page, size));
	}

	@Operation(summary = "댓글 작성", description = "익명 글의 작성자가 쓴 댓글은 자동으로 익명이 된다.")
	@ApiResponse(responseCode = "201")
	@ApiResponse(responseCode = "400", description = "댓글 길이 오류 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "게시글 없음 (POST_NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping
	public ResponseEntity<CommentResponse> create(
			@CurrentUser Long userId,
			@Parameter(description = "게시글 ID") @PathVariable Long postId,
			@Valid @RequestBody CommentCreateRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED).body(commentService.create(userId, postId, request));
	}

	@Operation(summary = "댓글 삭제", description = "작성자 또는 관리자가 삭제할 수 있다.")
	@ApiResponse(responseCode = "204")
	@ApiResponse(responseCode = "403", description = "권한 없음 (NOT_OWNER)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "게시글 또는 댓글 없음 (POST_NOT_FOUND, COMMENT_NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@DeleteMapping("/{commentId}")
	public ResponseEntity<Void> delete(
			@CurrentUser Long userId,
			@Parameter(description = "게시글 ID") @PathVariable Long postId,
			@Parameter(description = "댓글 ID") @PathVariable Long commentId
	) {
		commentService.delete(userId, postId, commentId);
		return ResponseEntity.noContent().build();
	}
}
