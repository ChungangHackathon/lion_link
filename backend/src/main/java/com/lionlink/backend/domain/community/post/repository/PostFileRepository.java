package com.lionlink.backend.domain.community.post.repository;

import com.lionlink.backend.domain.community.post.entity.PostFile;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostFileRepository extends JpaRepository<PostFile, Long> {

	List<PostFile> findByPostIdInOrderBySortOrderAscIdAsc(Collection<Long> postIds);

	List<PostFile> findByPostIdOrderBySortOrderAscIdAsc(Long postId);

	void deleteByPostId(Long postId);

	@Query("select count(f) > 0 from PostFile f where f.url = :url and f.post.id <> :postId and f.post.deletedAt is null")
	boolean isUsedByOtherPost(String url, Long postId);

	@Query("select count(p) > 0 from Profile p where p.photoUrl = :url")
	boolean isUsedAsProfilePhoto(String url);
}
