package com.lionlink.backend.domain.community.post.entity;

import com.lionlink.backend.global.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseCreatedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long authorId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private PostCategory category;

	@Column(nullable = false, length = 50)
	private String title;

	@Lob
	@Column(nullable = false)
	private String content;

	@JdbcTypeCode(SqlTypes.JSON)
	private List<String> links;

	private boolean isAnonymous;

	private int viewCount;

	// 수정됐을 때만 값이 생긴다. Auditing은 생성 시에도 채우므로 BaseTimeEntity를 쓰지 않는다
	private LocalDateTime updatedAt;

	private LocalDateTime deletedAt;

	@Builder
	private Post(Long authorId, PostCategory category, String title, String content,
			List<String> links, boolean isAnonymous) {
		this.authorId = authorId;
		this.category = category;
		this.title = title;
		this.content = content;
		this.links = links;
		this.isAnonymous = isAnonymous;
	}
}
