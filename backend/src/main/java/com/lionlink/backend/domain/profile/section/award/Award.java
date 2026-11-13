package com.lionlink.backend.domain.profile.section.award;

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
@Table(name = "awards")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Award extends ProfileSectionItem {

	@Column(nullable = false, length = 50)
	private String name;

	@Column(length = 50)
	private String organization;

	private YearMonth date;

	@Column(length = 100)
	private String description;

	@Builder
	private Award(Long userId, String name, String organization, YearMonth date, String description) {
		super(userId);
		this.name = name;
		this.organization = organization;
		this.date = date;
		this.description = description;
	}

	/** PROFILE-11 PUT: 보낸 값으로 통째로 바꾼다. */
	public void update(String name, String organization, YearMonth date, String description) {
		this.name = name;
		this.organization = organization;
		this.date = date;
		this.description = description;
	}
}
