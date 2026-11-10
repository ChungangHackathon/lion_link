package com.lionlink.backend.domain.profile.section.language;

import com.lionlink.backend.domain.profile.section.ProfileSectionItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
		name = "languages",
		uniqueConstraints = @UniqueConstraint(name = "uk_languages_user_language", columnNames = {"user_id", "language"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Language extends ProfileSectionItem {

	@Column(nullable = false, length = 30)
	private String language;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private LanguageLevel level;

	@Builder
	private Language(Long userId, String language, LanguageLevel level) {
		super(userId);
		this.language = language;
		this.level = level;
	}
}
