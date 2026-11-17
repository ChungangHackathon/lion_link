package com.lionlink.backend.domain.community.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "post_search_keywords",
		uniqueConstraints = @UniqueConstraint(name = "uk_post_search_keywords_user_keyword", columnNames = {"user_id", "keyword"}),
		indexes = @Index(name = "idx_post_search_keywords_searched_at", columnList = "searched_at"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchKeyword {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false, length = 20)
	private String keyword;

	@Column(nullable = false)
	private LocalDateTime searchedAt;
}
