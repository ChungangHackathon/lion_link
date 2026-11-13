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
import java.util.Objects;
import java.util.stream.Stream;
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

	public List<LionPart> getLionParts() {
		return Stream.of(lionPart1, lionPart2)
				.filter(Objects::nonNull)
				.toList();
	}

	/** PROFILE-04: 기본 정보를 받은 값으로 통째로 바꾼다. 경력·멋사 활동 같은 다른 테이블은 건드리지 않는다. */
	public void updateBasic(String headline, String major, String minor, String region, ProfileStatus status,
			List<LionPart> lionParts, List<String> interests) {
		this.headline = headline;
		this.major = major;
		this.minor = minor;
		this.region = region;
		this.status = status;
		this.lionPart1 = lionParts.get(0);
		this.lionPart2 = lionParts.size() > 1 ? lionParts.get(1) : null;
		this.interests = interests;
	}

	/** PROFILE-06: 비우면 null. */
	public void updateBio(String bio) {
		this.bio = bio;
	}

	/** PROFILE-07: 비어 있으면 null로 저장해 섹션이 사라지게 한다. */
	public void updateSkills(List<String> skills) {
		this.skills = skills.isEmpty() ? null : skills;
	}

	/** PROFILE-08: 비어 있으면 null로 저장해 섹션이 사라지게 한다. */
	public void updateLinks(List<ProfileLink> links) {
		this.links = links.isEmpty() ? null : links;
	}

	/** PROFILE-09: 연락처 이메일은 보여주기용이라 users.email과 별개다. 전화번호는 null이면 삭제. */
	public void updateContact(String contactEmail, String phone) {
		this.contactEmail = contactEmail;
		this.phone = phone;
	}

	/** PROFILE-10: null인 키는 바꾸지 않는다. */
	public void updateVisibility(Visibility email, Visibility phone, Visibility links, Visibility company) {
		if (email != null) {
			this.emailVisibility = email;
		}
		if (phone != null) {
			this.phoneVisibility = phone;
		}
		if (links != null) {
			this.linksVisibility = links;
		}
		if (company != null) {
			this.companyVisibility = company;
		}
	}
}
