package com.lionlink.backend.domain.alumni.repository;

import com.lionlink.backend.domain.alumni.entity.Bookmark;
import com.lionlink.backend.domain.alumni.entity.BookmarkId;
import java.util.Collection;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BookmarkRepository extends JpaRepository<Bookmark, BookmarkId> {

	Slice<Bookmark> findByUserIdOrderByCreatedAtDescTargetUserIdDesc(Long userId, Pageable pageable);

	@Query("select b.targetUserId from Bookmark b where b.userId = :userId and b.targetUserId in :targetUserIds")
	Set<Long> findTargetUserIds(Long userId, Collection<Long> targetUserIds);
}
