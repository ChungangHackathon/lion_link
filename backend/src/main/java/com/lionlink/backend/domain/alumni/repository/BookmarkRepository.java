package com.lionlink.backend.domain.alumni.repository;

import com.lionlink.backend.domain.alumni.entity.Bookmark;
import com.lionlink.backend.domain.alumni.entity.BookmarkId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, BookmarkId> {

	Slice<Bookmark> findByUserIdOrderByCreatedAtDescTargetUserIdDesc(Long userId, Pageable pageable);
}
