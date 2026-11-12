package com.lionlink.backend.domain.community.post.repository;

import com.lionlink.backend.domain.community.post.entity.PostFile;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostFileRepository extends JpaRepository<PostFile, Long> {

	List<PostFile> findByPostIdInOrderBySortOrderAscIdAsc(Collection<Long> postIds);

	List<PostFile> findByPostIdOrderBySortOrderAscIdAsc(Long postId);
}
