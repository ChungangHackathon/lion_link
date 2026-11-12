package com.lionlink.backend.domain.community.post.controller;

import com.lionlink.backend.domain.community.post.dto.response.PostLikeResponse;
import com.lionlink.backend.domain.community.post.service.PostLikeService;
import com.lionlink.backend.global.error.ErrorResponse;
import com.lionlink.backend.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "커뮤니티 게시글")
@RestController
@RequestMapping("/posts/{postId}/like")
@RequiredArgsConstructor
public class PostLikeController {

	private final PostLikeService postLikeService;

	@Operation(summary = "게시글 좋아요", description = "이미 좋아요한 글이면 그대로 성공한다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "404", description = "게시글 없음 (POST_NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PutMapping
	public PostLikeResponse like(
			@CurrentUser Long userId,
			@Parameter(description = "게시글 ID") @PathVariable Long postId
	) {
		return postLikeService.like(userId, postId);
	}

	@Operation(summary = "게시글 좋아요 취소", description = "좋아요하지 않은 글이어도 그대로 성공한다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "404", description = "게시글 없음 (POST_NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@DeleteMapping
	public PostLikeResponse unlike(
			@CurrentUser Long userId,
			@Parameter(description = "게시글 ID") @PathVariable Long postId
	) {
		return postLikeService.unlike(userId, postId);
	}
}
