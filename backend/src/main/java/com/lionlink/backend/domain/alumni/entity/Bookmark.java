package com.lionlink.backend.domain.alumni.entity;

import com.lionlink.backend.global.entity.BaseCreatedEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "bookmarks")
@IdClass(BookmarkId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bookmark extends BaseCreatedEntity {

	@Id
	private Long userId;

	@Id
	private Long targetUserId;

	@Builder
	private Bookmark(Long userId, Long targetUserId) {
		this.userId = userId;
		this.targetUserId = targetUserId;
	}
}
