package com.lionlink.backend.domain.profile.section.lionproject;

import com.lionlink.backend.domain.profile.section.ProfileSectionItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "lion_projects")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LionProject extends ProfileSectionItem {

	@Column(nullable = false, length = 30)
	private String name;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private LionEventType eventType;

	@Column(nullable = false)
	private Integer generation;

	@Column(length = 30)
	private String myRole;

	@Column(length = 200)
	private String description;

	@Column(length = 500)
	private String link;

	@Builder
	private LionProject(Long userId, String name, LionEventType eventType, Integer generation,
			String myRole, String description, String link) {
		super(userId);
		this.name = name;
		this.eventType = eventType;
		this.generation = generation;
		this.myRole = myRole;
		this.description = description;
		this.link = link;
	}
}
