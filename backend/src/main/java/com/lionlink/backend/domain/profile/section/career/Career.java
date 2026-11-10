package com.lionlink.backend.domain.profile.section.career;

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
@Table(name = "careers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Career extends ProfileSectionItem {

	@Column(nullable = false, length = 30)
	private String company;

	@Column(nullable = false, length = 30)
	private String title;

	@Column(length = 20)
	private String employmentType;

	private YearMonth startYm;

	private YearMonth endYm;

	private boolean isCurrent;

	@Column(length = 300)
	private String description;

	@Builder
	private Career(Long userId, String company, String title, String employmentType,
			YearMonth startYm, YearMonth endYm, boolean isCurrent, String description) {
		super(userId);
		this.company = company;
		this.title = title;
		this.employmentType = employmentType;
		this.startYm = startYm;
		this.endYm = endYm;
		this.isCurrent = isCurrent;
		this.description = description;
	}
}
