package com.lionlink.backend.domain.profile.section.certification;

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
@Table(name = "certifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Certification extends ProfileSectionItem {

	@Column(nullable = false, length = 50)
	private String name;

	@Column(length = 50)
	private String organization;

	private YearMonth date;

	@Builder
	private Certification(Long userId, String name, String organization, YearMonth date) {
		super(userId);
		this.name = name;
		this.organization = organization;
		this.date = date;
	}
}
