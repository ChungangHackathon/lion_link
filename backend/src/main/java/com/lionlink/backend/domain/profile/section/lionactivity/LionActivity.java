package com.lionlink.backend.domain.profile.section.lionactivity;

import com.lionlink.backend.domain.profile.LionPart;
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
		name = "lion_activities",
		uniqueConstraints = @UniqueConstraint(name = "uk_lion_activities_user_generation", columnNames = {"user_id", "generation"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LionActivity extends ProfileSectionItem {

	@Column(nullable = false)
	private Integer generation;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private LionRole role;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "part_1", nullable = false, length = 20)
	private LionPart part1;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "part_2", length = 20)
	private LionPart part2;

	private boolean isDefault;

	@Builder
	private LionActivity(Long userId, Integer generation, LionRole role, LionPart part1, LionPart part2, boolean isDefault) {
		super(userId);
		this.generation = generation;
		this.role = role;
		this.part1 = part1;
		this.part2 = part2;
		this.isDefault = isDefault;
	}

	/** PUT: 일반 항목은 기수·역할·파트를 모두 바꾼다. */
	public void update(Integer generation, LionRole role, LionPart part1, LionPart part2) {
		this.generation = generation;
		this.role = role;
		this.part1 = part1;
		this.part2 = part2;
	}

	/** PUT: 가입 기수 항목(isDefault)은 역할·파트만 바꾼다. 기수는 가입 정보와 동기화되므로 못 바꾼다. */
	public void updateRoleAndParts(LionRole role, LionPart part1, LionPart part2) {
		this.role = role;
		this.part1 = part1;
		this.part2 = part2;
	}

	/** 가입 기수가 바뀌면(AUTH-06·07, ADMIN-06) 자동 생성 항목의 기수도 같이 바꾼다. 회원이 직접 기수를 바꾸는 데 쓰지 않는다. */
	public void changeGeneration(Integer generation) {
		this.generation = generation;
	}
}
