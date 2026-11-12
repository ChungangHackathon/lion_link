package com.lionlink.backend.domain.community.post.repository;

import com.lionlink.backend.domain.community.post.entity.Post;
import com.lionlink.backend.domain.community.post.entity.PostCategory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, Long> {

	Optional<Post> findByIdAndDeletedAtIsNull(Long id);

	Optional<Post> findFirstByDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByViewCountDescIdDesc(
			LocalDateTime from, LocalDateTime to);

	@Modifying(clearAutomatically = true)
	@Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :postId")
	void increaseViewCount(Long postId);

	@Query("select p.category as category, count(p) as count from Post p where p.deletedAt is null group by p.category")
	List<CategoryCount> countByCategory();

	interface CategoryCount {

		PostCategory getCategory();

		Long getCount();
	}
}
