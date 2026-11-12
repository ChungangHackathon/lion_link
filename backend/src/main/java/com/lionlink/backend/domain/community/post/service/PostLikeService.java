package com.lionlink.backend.domain.community.post.service;

import com.lionlink.backend.domain.community.exception.CommunityErrorCode;
import com.lionlink.backend.domain.community.post.dto.response.PostLikeResponse;
import com.lionlink.backend.domain.community.post.entity.Post;
import com.lionlink.backend.domain.community.post.entity.PostLike;
import com.lionlink.backend.domain.community.post.entity.PostLikeId;
import com.lionlink.backend.domain.community.post.repository.PostLikeRepository;
import com.lionlink.backend.domain.community.post.repository.PostRepository;
import com.lionlink.backend.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostLikeService {

	private final PostRepository postRepository;
	private final PostLikeRepository postLikeRepository;

	public PostLikeResponse like(Long userId, Long postId) {
		Post post = findPost(postId);
		PostLikeId id = new PostLikeId(postId, userId);
		if (!postLikeRepository.existsById(id)) {
			try {
				postLikeRepository.saveAndFlush(PostLike.builder().post(post).userId(userId).build());
			} catch (DataIntegrityViolationException e) {
				if (!postLikeRepository.existsById(id)) {
					throw e;
				}
			}
		}
		return new PostLikeResponse(postId, true, postLikeRepository.countByPostId(postId));
	}

	@Transactional
	public PostLikeResponse unlike(Long userId, Long postId) {
		findPost(postId);
		postLikeRepository.deleteById(new PostLikeId(postId, userId));
		postLikeRepository.flush();
		return new PostLikeResponse(postId, false, postLikeRepository.countByPostId(postId));
	}

	private Post findPost(Long postId) {
		return postRepository.findByIdAndDeletedAtIsNull(postId)
				.orElseThrow(() -> new BusinessException(CommunityErrorCode.POST_NOT_FOUND));
	}
}
