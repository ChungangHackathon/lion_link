package com.lionlink.backend.domain.community.comment.dto.response;

import com.lionlink.backend.domain.community.comment.entity.Comment;
import com.lionlink.backend.domain.community.post.dto.response.PostAuthorResponse;
import com.lionlink.backend.domain.profile.UserSummary;
import java.time.LocalDateTime;

public record CommentResponse(
		Long id,
		Long postId,
		String content,
		boolean isAnonymous,
		PostAuthorResponse author,
		boolean deleted,
		boolean isMine,
		LocalDateTime createdAt
) {

	private static final String DELETED_CONTENT = "삭제된 댓글이에요.";

	public static CommentResponse of(Comment comment, Long postId, UserSummary author, Long userId) {
		boolean hidden = comment.isDeleted() || comment.isAnonymous();
		return new CommentResponse(
				comment.getId(),
				postId,
				comment.isDeleted() ? DELETED_CONTENT : comment.getContent(),
				comment.isAnonymous(),
				hidden ? null : PostAuthorResponse.from(author),
				comment.isDeleted(),
				comment.isWrittenBy(userId),
				comment.getCreatedAt()
		);
	}
}
