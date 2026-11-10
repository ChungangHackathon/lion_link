package com.lionlink.backend.domain.community.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "post_files")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostFile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private PostFileType type;

	@Column(nullable = false, length = 500)
	private String url;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private Integer size;

	private int sortOrder;

	@Builder
	private PostFile(Post post, PostFileType type, String url, String name, Integer size, int sortOrder) {
		this.post = post;
		this.type = type;
		this.url = url;
		this.name = name;
		this.size = size;
		this.sortOrder = sortOrder;
	}
}
