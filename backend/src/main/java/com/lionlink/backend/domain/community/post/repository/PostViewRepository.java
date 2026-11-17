package com.lionlink.backend.domain.community.post.repository;

import com.lionlink.backend.domain.community.post.entity.PostView;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostViewRepository extends JpaRepository<PostView, Long> {

	boolean existsByPostIdAndUserIdAndViewedAtAfter(Long postId, Long userId, LocalDateTime after);
}
