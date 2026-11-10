package com.lionlink.backend.domain.profile.basic.entity;

import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.ProfileStatus;
import com.lionlink.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile extends BaseTimeEntity {

	@Id
	private Long userId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "lion_part_1", nullable = false, length = 20)
	private LionPart lionPart1;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "lion_part_2", length = 20)
	private LionPart lionPart2;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private ProfileStatus status;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private List<String> interests;

	@JdbcTypeCode(SqlTypes.JSON)
	private List<String> skills;

	@Column(length = 500)
	private String photoUrl;

	@Column(length = 100)
	private String headline;

	@Column(length = 1000)
	private String bio;

	@Column(length = 30)
	private String major;

	@Column(length = 30)
	private String minor;

	@Column(length = 20)
	private String region;

	@JdbcTypeCode(SqlTypes.JSON)
	private List<ProfileLink> links;

	private String contactEmail;

	@Column(length = 20)
	private String phone;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private Visibility emailVisibility;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private Visibility phoneVisibility;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private Visibility linksVisibility;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private Visibility companyVisibility;

	@Builder
	private Profile(Long userId, LionPart lionPart1, LionPart lionPart2, ProfileStatus status,
			List<String> interests, List<String> skills, String photoUrl, String headline, String bio,
			String major, String minor, String region, List<ProfileLink> links, String contactEmail, String phone) {
		this.userId = userId;
		this.lionPart1 = lionPart1;
		this.lionPart2 = lionPart2;
		this.status = status;
		this.interests = interests;
		this.skills = skills;
		this.photoUrl = photoUrl;
		this.headline = headline;
		this.bio = bio;
		this.major = major;
		this.minor = minor;
		this.region = region;
		this.links = links;
		this.contactEmail = contactEmail;
		this.phone = phone;
		this.emailVisibility = Visibility.CONNECTED;
		this.phoneVisibility = Visibility.CONNECTED;
		this.linksVisibility = Visibility.PUBLIC;
		this.companyVisibility = Visibility.PUBLIC;
	}
}
