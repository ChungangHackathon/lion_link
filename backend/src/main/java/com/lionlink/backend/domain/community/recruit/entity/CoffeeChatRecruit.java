package com.lionlink.backend.domain.community.recruit.entity;

import com.lionlink.backend.domain.community.post.entity.Post;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "coffeechat_recruits")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoffeeChatRecruit {

	@Id
	private Long postId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id")
	private Post post;

	@Column(nullable = false)
	private Long seniorId;

	@Column(nullable = false)
	private Integer capacity;

	@Column(nullable = false)
	private LocalDate deadline;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private RecruitStatus status;

	private Long coffeeChatId;

	@Builder
	private CoffeeChatRecruit(Post post, Long seniorId, Integer capacity, LocalDate deadline) {
		this.post = post;
		this.seniorId = seniorId;
		this.capacity = capacity;
		this.deadline = deadline;
		this.status = RecruitStatus.OPEN;
	}
}
