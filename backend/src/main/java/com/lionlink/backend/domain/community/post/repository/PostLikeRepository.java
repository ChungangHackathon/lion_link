package com.lionlink.backend.domain.community.post.repository;

import com.lionlink.backend.domain.community.post.entity.PostLike;
import com.lionlink.backend.domain.community.post.entity.PostLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, PostLikeId> {

	long countByPostId(Long postId);
}
