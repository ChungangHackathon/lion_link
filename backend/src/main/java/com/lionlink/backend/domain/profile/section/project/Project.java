package com.lionlink.backend.domain.profile.section.project;

import com.lionlink.backend.domain.profile.section.ProfileSectionItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.YearMonth;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "projects")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends ProfileSectionItem {

	@Column(nullable = false, length = 50)
	private String name;

	private YearMonth startYm;

	private YearMonth endYm;

	private boolean isCurrent;

	@Column(length = 30)
	private String role;

	@Column(length = 300)
	private String description;

	@Column(length = 500)
	private String link;

	@JdbcTypeCode(SqlTypes.JSON)
	private List<String> tags;

	@Builder
	private Project(Long userId, String name, YearMonth startYm, YearMonth endYm, boolean isCurrent,
			String role, String description, String link, List<String> tags) {
		super(userId);
		this.name = name;
		this.startYm = startYm;
		this.endYm = endYm;
		this.isCurrent = isCurrent;
		this.role = role;
		this.description = description;
		this.link = link;
		this.tags = tags;
	}

	/** PROFILE-11 PUT: 보낸 값으로 통째로 바꾼다. 진행 중이면 종료 년월은 비운다. */
	public void update(String name, YearMonth startYm, YearMonth endYm, boolean isCurrent,
			String role, String description, String link, List<String> tags) {
		this.name = name;
		this.startYm = startYm;
		this.endYm = isCurrent ? null : endYm;
		this.isCurrent = isCurrent;
		this.role = role;
		this.description = description;
		this.link = link;
		this.tags = tags;
	}
}
