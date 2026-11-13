package com.lionlink.backend.domain.profile.section.activity;

import com.lionlink.backend.domain.profile.section.ProfileSectionItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.YearMonth;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "activities")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Activity extends ProfileSectionItem {

	@Column(nullable = false, length = 50)
	private String name;

	@Column(length = 50)
	private String organization;

	private YearMonth startYm;

	private YearMonth endYm;

	@Column(length = 200)
	private String description;

	@Builder
	private Activity(Long userId, String name, String organization,
			YearMonth startYm, YearMonth endYm, String description) {
		super(userId);
		this.name = name;
		this.organization = organization;
		this.startYm = startYm;
		this.endYm = endYm;
		this.description = description;
	}

	/** PROFILE-11 PUT: 보낸 값으로 통째로 바꾼다. */
	public void update(String name, String organization, YearMonth startYm, YearMonth endYm, String description) {
		this.name = name;
		this.organization = organization;
		this.startYm = startYm;
		this.endYm = endYm;
		this.description = description;
	}
}
