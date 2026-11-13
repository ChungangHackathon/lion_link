package com.lionlink.backend.domain.profile.basic.dto.response;

import com.lionlink.backend.domain.profile.basic.entity.Profile;
import com.lionlink.backend.domain.profile.basic.entity.ProfileLink;
import com.lionlink.backend.domain.profile.section.activity.Activity;
import com.lionlink.backend.domain.profile.section.award.Award;
import com.lionlink.backend.domain.profile.section.career.Career;
import com.lionlink.backend.domain.profile.section.certification.Certification;
import com.lionlink.backend.domain.profile.section.education.Education;
import com.lionlink.backend.domain.profile.section.language.Language;
import com.lionlink.backend.domain.profile.section.lionactivity.LionActivity;
import com.lionlink.backend.domain.profile.section.lionproject.LionProject;
import com.lionlink.backend.domain.profile.section.project.Project;
import com.lionlink.backend.domain.user.entity.User;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** PROFILE-02 내 프로필. 공개 범위를 적용하지 않은 전체 내용과 현재 공개 범위 설정을 담는다. */
public record ProfileResponse(
		Long userId,
		String name,
		Integer generation,
		String photoUrl,
		String headline,
		String bio,
		String major,
		String minor,
		String region,
		String status,
		List<String> lionParts,
		List<String> interests,
		List<String> skills,
		List<LinkItem> links,
		Contact contact,
		VisibilitySettings visibility,
		List<CareerItem> careers,
		List<EducationItem> educations,
		List<LionActivityItem> lionActivities,
		List<LionProjectItem> lionProjects,
		List<ProjectItem> projects,
		List<CertificationItem> certifications,
		List<AwardItem> awards,
		List<ActivityItem> activities,
		List<LanguageItem> languages
) {

	/** 섹션 목록은 저장소 쿼리가 이미 정렬해서 넘긴다. */
	public record Sections(
			List<Career> careers,
			List<Education> educations,
			List<LionActivity> lionActivities,
			List<LionProject> lionProjects,
			List<Project> projects,
			List<Certification> certifications,
			List<Award> awards,
			List<Activity> activities,
			List<Language> languages
	) {
	}

	public static ProfileResponse of(User user, Profile profile, Sections sections) {
		return new ProfileResponse(
				user.getId(),
				user.getName(),
				user.getGeneration(),
				profile.getPhotoUrl(),
				profile.getHeadline(),
				profile.getBio(),
				profile.getMajor(),
				profile.getMinor(),
				profile.getRegion(),
				profile.getStatus().name(),
				profile.getLionParts().stream().map(Enum::name).toList(),
				orEmpty(profile.getInterests()),
				orEmpty(profile.getSkills()),
				orEmpty(profile.getLinks()).stream().map(LinkItem::from).toList(),
				new Contact(profile.getContactEmail(), profile.getPhone()),
				new VisibilitySettings(
						profile.getEmailVisibility().name(),
						profile.getPhoneVisibility().name(),
						profile.getLinksVisibility().name(),
						profile.getCompanyVisibility().name()),
				sections.careers().stream().map(CareerItem::from).toList(),
				sections.educations().stream().map(EducationItem::from).toList(),
				sections.lionActivities().stream().map(LionActivityItem::from).toList(),
				sections.lionProjects().stream().map(LionProjectItem::from).toList(),
				sections.projects().stream().map(ProjectItem::from).toList(),
				sections.certifications().stream().map(CertificationItem::from).toList(),
				sections.awards().stream().map(AwardItem::from).toList(),
				sections.activities().stream().map(ActivityItem::from).toList(),
				sections.languages().stream().map(LanguageItem::from).toList());
	}

	private static <T> List<T> orEmpty(List<T> values) {
		return values == null ? List.of() : values;
	}

	public record LinkItem(String type, String url) {

		static LinkItem from(ProfileLink link) {
			return new LinkItem(link.type(), link.url());
		}
	}

	public record Contact(String email, String phone) {
	}

	public record VisibilitySettings(String email, String phone, String links, String company) {
	}

	public record CareerItem(Long id, String company, String jobTitle, String employmentType,
			YearMonth startYm, YearMonth endYm, boolean isCurrent, String description) {

		static CareerItem from(Career career) {
			return new CareerItem(career.getId(), career.getCompany(), career.getTitle(), career.getEmploymentType(),
					career.getStartYm(), career.getEndYm(), career.isCurrent(), career.getDescription());
		}
	}

	public record EducationItem(Long id, String school, String major, String degree,
			YearMonth startYm, YearMonth endYm, boolean isCurrent, String description) {

		static EducationItem from(Education education) {
			return new EducationItem(education.getId(), education.getSchool(), education.getMajor(),
					education.getDegree() == null ? null : education.getDegree().name(),
					education.getStartYm(), education.getEndYm(), education.isCurrent(), education.getDescription());
		}
	}

	public record LionActivityItem(Long id, Integer generation, String role, List<String> parts, boolean isDefault) {

		static LionActivityItem from(LionActivity activity) {
			List<String> parts = Stream.of(activity.getPart1(), activity.getPart2())
					.filter(Objects::nonNull)
					.map(Enum::name)
					.toList();
			return new LionActivityItem(activity.getId(), activity.getGeneration(), activity.getRole().name(),
					parts, activity.isDefault());
		}
	}

	public record LionProjectItem(Long id, String name, String eventType, Integer generation,
			String myRole, String description, String link) {

		static LionProjectItem from(LionProject project) {
			return new LionProjectItem(project.getId(), project.getName(), project.getEventType().name(),
					project.getGeneration(), project.getMyRole(), project.getDescription(), project.getLink());
		}
	}

	public record ProjectItem(Long id, String name, YearMonth startYm, YearMonth endYm, boolean isCurrent,
			String role, String description, String link, List<String> tags) {

		static ProjectItem from(Project project) {
			return new ProjectItem(project.getId(), project.getName(), project.getStartYm(), project.getEndYm(),
					project.isCurrent(), project.getRole(), project.getDescription(), project.getLink(),
					orEmpty(project.getTags()));
		}
	}

	public record CertificationItem(Long id, String name, String issuer, YearMonth acquiredYm) {

		static CertificationItem from(Certification certification) {
			return new CertificationItem(certification.getId(), certification.getName(),
					certification.getOrganization(), certification.getDate());
		}
	}

	public record AwardItem(Long id, String name, String host, YearMonth awardedYm, String description) {

		static AwardItem from(Award award) {
			return new AwardItem(award.getId(), award.getName(), award.getOrganization(), award.getDate(),
					award.getDescription());
		}
	}

	public record ActivityItem(Long id, String name, String organization, YearMonth startYm, YearMonth endYm,
			String description) {

		static ActivityItem from(Activity activity) {
			return new ActivityItem(activity.getId(), activity.getName(), activity.getOrganization(),
					activity.getStartYm(), activity.getEndYm(), activity.getDescription());
		}
	}

	public record LanguageItem(Long id, String language, String level) {

		static LanguageItem from(Language language) {
			return new LanguageItem(language.getId(), language.getLanguage(), language.getLevel().name());
		}
	}
}
