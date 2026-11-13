package com.lionlink.backend.domain.profile.section.career;

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
@Table(name = "careers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Career extends ProfileSectionItem {

	@Column(nullable = false, length = 30)
	private String company;

	@Column(nullable = false, length = 30)
	private String title;

	/** 온보딩에서 자동 생성된 경력은 null. 회원이 수정해 저장할 때 채운다. */
	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(length = 20)
	private EmploymentType employmentType;

	private YearMonth startYm;

	private YearMonth endYm;

	private boolean isCurrent;

	@Column(length = 300)
	private String description;

	@Builder
	private Career(Long userId, String company, String title, EmploymentType employmentType,
			YearMonth startYm, YearMonth endYm, boolean isCurrent, String description) {
		super(userId);
		this.company = company;
		this.title = title;
		this.employmentType = employmentType;
		this.startYm = startYm;
		this.endYm = isCurrent ? null : endYm;
		this.isCurrent = isCurrent;
		this.description = description;
	}

	/** PROFILE-11 PUT: 보낸 값으로 통째로 바꾼다. 진행 중이면 종료 년월은 비운다. */
	public void update(String company, String title, EmploymentType employmentType,
			YearMonth startYm, YearMonth endYm, boolean isCurrent, String description) {
		this.company = company;
		this.title = title;
		this.employmentType = employmentType;
		this.startYm = startYm;
		this.endYm = isCurrent ? null : endYm;
		this.isCurrent = isCurrent;
		this.description = description;
	}
}
