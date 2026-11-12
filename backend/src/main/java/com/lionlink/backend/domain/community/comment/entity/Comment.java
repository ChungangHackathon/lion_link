package com.lionlink.backend.domain.community.comment.entity;

import com.lionlink.backend.domain.community.post.entity.Post;
import com.lionlink.backend.global.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseCreatedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;

	@Column(nullable = false)
	private Long authorId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	private Comment parent;

	@Column(nullable = false, length = 500)
	private String content;

	private boolean isAnonymous;

	private Integer anonNo;

	private LocalDateTime deletedAt;

	@Builder
	private Comment(Post post, Long authorId, Comment parent, String content, boolean isAnonymous, Integer anonNo) {
		this.post = post;
		this.authorId = authorId;
		this.parent = parent;
		this.content = content;
		this.isAnonymous = isAnonymous;
		this.anonNo = anonNo;
	}

	public void delete() {
		this.deletedAt = LocalDateTime.now();
	}

	public boolean isDeleted() {
		return deletedAt != null;
	}

	public boolean isWrittenBy(Long userId) {
		return authorId.equals(userId);
	}
}
