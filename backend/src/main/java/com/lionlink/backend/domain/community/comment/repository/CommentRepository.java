package com.lionlink.backend.domain.community.comment.repository;

import com.lionlink.backend.domain.community.comment.entity.Comment;
import com.lionlink.backend.global.jpa.IdCount;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	@Query("""
			select c.post.id as id, count(c) as count
			from Comment c
			where c.post.id in :postIds and c.deletedAt is null
			group by c.post.id
			""")
	List<IdCount> countByPostIds(Collection<Long> postIds);

	long countByPostIdAndDeletedAtIsNull(Long postId);

	Slice<Comment> findByPostIdOrderByCreatedAtAscIdAsc(Long postId, Pageable pageable);

	Optional<Comment> findByIdAndPostIdAndDeletedAtIsNull(Long id, Long postId);
}
