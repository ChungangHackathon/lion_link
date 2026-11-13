package com.lionlink.backend.domain.profile.section.education;

import com.lionlink.backend.domain.profile.section.ProfileSectionItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.YearMonth;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "educations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Education extends ProfileSectionItem {

	@Column(nullable = false, length = 30)
	private String school;

	@Column(length = 30)
	private String major;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(length = 20)
	private Degree degree;

	private YearMonth startYm;

	private YearMonth endYm;

	private boolean isCurrent;

	@Column(length = 200)
	private String description;

	@Builder
	private Education(Long userId, String school, String major, Degree degree,
			YearMonth startYm, YearMonth endYm, boolean isCurrent, String description) {
		super(userId);
		this.school = school;
		this.major = major;
		this.degree = degree;
		this.startYm = startYm;
		this.endYm = endYm;
		this.isCurrent = isCurrent;
		this.description = description;
	}

	/** PROFILE-11 PUT: 보낸 값으로 통째로 바꾼다. 진행 중이면 종료 년월은 비운다. */
	public void update(String school, String major, Degree degree,
			YearMonth startYm, YearMonth endYm, boolean isCurrent, String description) {
		this.school = school;
		this.major = major;
		this.degree = degree;
		this.startYm = startYm;
		this.endYm = isCurrent ? null : endYm;
		this.isCurrent = isCurrent;
		this.description = description;
	}
}
