package com.lionlink.backend.domain.community.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest(
		@NotBlank(message = "댓글을 입력해 주세요.")
		@Size(max = 500, message = "댓글은 1~500자여야 합니다.")
		String content
) {
}
