package com.lionlink.backend.domain.community.post.entity;

import com.lionlink.backend.global.entity.BaseCreatedEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "post_likes")
@IdClass(PostLikeId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike extends BaseCreatedEntity {

	@Id
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id")
	private Post post;

	@Id
	private Long userId;

	@Builder
	private PostLike(Post post, Long userId) {
		this.post = post;
		this.userId = userId;
	}
}
