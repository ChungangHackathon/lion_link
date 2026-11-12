package com.lionlink.backend.domain.community.meetup.entity;

import com.lionlink.backend.domain.community.post.entity.Post;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "meetups")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meetup {

	@Id
	private Long postId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id")
	private Post post;

	@Column(nullable = false)
	private LocalDateTime meetAt;

	@Column(nullable = false, length = 50)
	private String place;

	@Column(nullable = false)
	private Integer capacity;

	private LocalDateTime canceledAt;

	@Builder
	private Meetup(Post post, LocalDateTime meetAt, String place, Integer capacity) {
		this.post = post;
		this.meetAt = meetAt;
		this.place = place;
		this.capacity = capacity;
	}

	public void update(LocalDateTime meetAt, String place, Integer capacity) {
		this.meetAt = meetAt;
		this.place = place;
		this.capacity = capacity;
	}

	public void cancel() {
		if (canceledAt == null) {
			this.canceledAt = LocalDateTime.now();
		}
	}

	public boolean isCanceled() {
		return canceledAt != null;
	}

	public boolean isEnded(LocalDateTime now) {
		return !meetAt.isAfter(now);
	}
}
