package com.lionlink.backend.domain.community.comment.service;

import com.lionlink.backend.domain.community.comment.dto.request.CommentCreateRequest;
import com.lionlink.backend.domain.community.comment.dto.response.CommentResponse;
import com.lionlink.backend.domain.community.comment.entity.Comment;
import com.lionlink.backend.domain.community.comment.repository.CommentRepository;
import com.lionlink.backend.domain.community.event.CommentCreatedEvent;
import com.lionlink.backend.domain.community.exception.CommunityErrorCode;
import com.lionlink.backend.domain.community.post.entity.Post;
import com.lionlink.backend.domain.community.post.repository.PostRepository;
import com.lionlink.backend.domain.profile.UserSummary;
import com.lionlink.backend.domain.profile.UserSummaryReader;
import com.lionlink.backend.domain.user.entity.UserRole;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.response.SliceResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

	private final CommentRepository commentRepository;
	private final PostRepository postRepository;
	private final UserRepository userRepository;
	private final UserSummaryReader userSummaryReader;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional(readOnly = true)
	public SliceResponse<CommentResponse> getComments(Long userId, Long postId, Pageable pageable) {
		findPost(postId);
		Slice<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAscIdAsc(postId, pageable);
		Map<Long, UserSummary> authors = userSummaryReader.getSummaries(comments.stream()
				.filter(comment -> !comment.isDeleted() && !comment.isAnonymous())
				.map(Comment::getAuthorId)
				.distinct()
				.toList());
		return SliceResponse.from(comments, comment -> CommentResponse.of(comment, postId, author(authors, comment), userId));
	}

	public CommentResponse create(Long userId, Long postId, CommentCreateRequest request) {
		Post post = findPost(postId);
		Comment comment = commentRepository.save(Comment.builder()
				.post(post)
				.authorId(userId)
				.content(request.content().trim())
				.isAnonymous(post.isAnonymous() && post.isWrittenBy(userId))
				.build());
		UserSummary author = userSummaryReader.getSummary(userId).orElse(UserSummary.withdrawn(userId));
		if (!post.isWrittenBy(userId)) {
			Long commenterId = comment.isAnonymous() ? null : userId;
			eventPublisher.publishEvent(new CommentCreatedEvent(postId, post.getTitle(), post.getAuthorId(), commenterId));
		}
		return CommentResponse.of(comment, postId, author, userId);
	}

	public void delete(Long userId, Long postId, Long commentId) {
		findPost(postId);
		Comment comment = commentRepository.findByIdAndPostIdAndDeletedAtIsNull(commentId, postId)
				.orElseThrow(() -> new BusinessException(CommunityErrorCode.COMMENT_NOT_FOUND));
		if (!comment.isWrittenBy(userId) && !userRepository.existsByIdAndRole(userId, UserRole.ADMIN)) {
			throw new BusinessException(CommunityErrorCode.NOT_OWNER, "본인이 작성한 댓글만 삭제할 수 있어요.");
		}
		comment.delete();
	}

	private Post findPost(Long postId) {
		return postRepository.findByIdAndDeletedAtIsNull(postId)
				.orElseThrow(() -> new BusinessException(CommunityErrorCode.POST_NOT_FOUND));
	}

	private UserSummary author(Map<Long, UserSummary> authors, Comment comment) {
		return authors.getOrDefault(comment.getAuthorId(), UserSummary.withdrawn(comment.getAuthorId()));
	}
}
