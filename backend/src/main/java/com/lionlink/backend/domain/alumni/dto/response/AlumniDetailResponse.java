package com.lionlink.backend.domain.alumni.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lionlink.backend.domain.coffeechat.entity.AvailableSlot;
import com.lionlink.backend.domain.coffeechat.entity.MeetingMode;
import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.ProfileStatus;
import com.lionlink.backend.domain.profile.basic.entity.Profile;
import com.lionlink.backend.domain.profile.basic.entity.ProfileLink;
import com.lionlink.backend.domain.profile.section.activity.Activity;
import com.lionlink.backend.domain.profile.section.award.Award;
import com.lionlink.backend.domain.profile.section.career.Career;
import com.lionlink.backend.domain.profile.section.certification.Certification;
import com.lionlink.backend.domain.profile.section.education.Degree;
import com.lionlink.backend.domain.profile.section.education.Education;
import com.lionlink.backend.domain.profile.section.language.Language;
import com.lionlink.backend.domain.profile.section.language.LanguageLevel;
import com.lionlink.backend.domain.profile.section.lionactivity.LionActivity;
import com.lionlink.backend.domain.profile.section.lionactivity.LionRole;
import com.lionlink.backend.domain.profile.section.lionproject.LionEventType;
import com.lionlink.backend.domain.profile.section.lionproject.LionProject;
import com.lionlink.backend.domain.profile.section.project.Project;
import com.lionlink.backend.domain.user.entity.User;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public record AlumniDetailResponse(
		Long userId,
		String name,
		Integer generation,
		String photoUrl,
		String headline,
		String bio,
		String major,
		String minor,
		String region,
		ProfileStatus status,
		List<LionPart> lionParts,
		List<String> interests,
		List<String> skills,
		@JsonInclude(JsonInclude.Include.NON_NULL) List<ProfileLink> links,
		Contact contact,
		List<CareerResponse> careers,
		List<EducationResponse> educations,
		List<LionActivityResponse> lionActivities,
		List<LionProjectResponse> lionProjects,
		List<ProjectResponse> projects,
		List<CertificationResponse> certifications,
		List<AwardResponse> awards,
		List<ActivityResponse> activities,
		List<LanguageResponse> languages,
		boolean isStaff,
		CoffeeChat coffeeChat,
		Chat chat,
		List<Commonality> commonalities,
		long mutualCount,
		boolean bookmarked
) {

	public static AlumniDetailResponse of(User user, Profile profile, Sections sections, Visibility visibility,
			Relation relation) {
		return new AlumniDetailResponse(
				user.getId(),
				user.getName(),
				user.getGeneration(),
				profile.getPhotoUrl(),
				profile.getHeadline(),
				profile.getBio(),
				profile.getMajor(),
				profile.getMinor(),
				profile.getRegion(),
				profile.getStatus(),
				profile.getLionParts(),
				profile.getInterests(),
				Objects.requireNonNullElse(profile.getSkills(), List.of()),
				visibility.links() ? Objects.requireNonNullElse(profile.getLinks(), List.of()) : null,
				new Contact(
						visibility.email() ? profile.getContactEmail() : null,
						visibility.phone() ? profile.getPhone() : null
				),
				sections.careers().stream().map(career -> CareerResponse.of(career, visibility.company())).toList(),
				sections.educations().stream().map(EducationResponse::from).toList(),
				sections.lionActivities().stream().map(LionActivityResponse::from).toList(),
				sections.lionProjects().stream().map(LionProjectResponse::from).toList(),
				sections.projects().stream().map(ProjectResponse::from).toList(),
				sections.certifications().stream().map(CertificationResponse::from).toList(),
				sections.awards().stream().map(AwardResponse::from).toList(),
				sections.activities().stream().map(ActivityResponse::from).toList(),
				sections.languages().stream().map(LanguageResponse::from).toList(),
				relation.isStaff(),
				relation.coffeeChat(),
				relation.chat(),
				relation.commonalities(),
				relation.mutualCount(),
				relation.bookmarked()
		);
	}

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

	public record Visibility(boolean email, boolean phone, boolean links, boolean company) {
	}

	public record Relation(
			boolean isStaff,
			CoffeeChat coffeeChat,
			Chat chat,
			List<Commonality> commonalities,
			long mutualCount,
			boolean bookmarked
	) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Contact(String email, String phone) {
	}

	public record CoffeeChat(
			CoffeeChatAvailability availability,
			LocalDate nextAvailableDate,
			boolean acceptGroup,
			List<String> topics,
			List<MeetingMode> modes,
			List<AvailableSlot> availableSlots,
			String preferredArea,
			Long pendingCoffeeChatId
	) {
	}

	public enum CoffeeChatAvailability {
		AVAILABLE,
		RESTING,
		NOT_ACCEPTING,
		IN_PROGRESS,
		COOLDOWN
	}

	public record Chat(ChatStatus status, LocalDate requestAvailableDate) {
	}

	public enum ChatStatus {
		NONE,
		REQUESTED,
		ACTIVE
	}

	public record Commonality(CommonalityType type, String label) {
	}

	public enum CommonalityType {
		SAME_LION_PROJECT,
		SAME_COMPANY,
		SAME_GENERATION,
		SAME_PART,
		SHARED_INTEREST
	}

	public record CareerResponse(
			Long id,
			@JsonInclude(JsonInclude.Include.NON_NULL) String company,
			boolean companyHidden,
			String jobTitle,
			String employmentType,
			YearMonth startYm,
			YearMonth endYm,
			boolean isCurrent,
			String description
	) {

		static CareerResponse of(Career career, boolean companyVisible) {
			return new CareerResponse(
					career.getId(),
					companyVisible ? career.getCompany() : null,
					!companyVisible,
					career.getTitle(),
					career.getEmploymentType() == null ? null : career.getEmploymentType().name(),
					career.getStartYm(),
					career.getEndYm(),
					career.isCurrent(),
					career.getDescription()
			);
		}
	}

	public record EducationResponse(
			Long id,
			String school,
			String major,
			Degree degree,
			YearMonth startYm,
			YearMonth endYm,
			boolean isCurrent,
			String description
	) {

		static EducationResponse from(Education education) {
			return new EducationResponse(
					education.getId(),
					education.getSchool(),
					education.getMajor(),
					education.getDegree(),
					education.getStartYm(),
					education.getEndYm(),
					education.isCurrent(),
					education.getDescription()
			);
		}
	}

	public record LionActivityResponse(
			Long id,
			Integer generation,
			LionRole role,
			List<LionPart> parts,
			boolean isDefault
	) {

		static LionActivityResponse from(LionActivity activity) {
			return new LionActivityResponse(
					activity.getId(),
					activity.getGeneration(),
					activity.getRole(),
					Stream.of(activity.getPart1(), activity.getPart2()).filter(Objects::nonNull).toList(),
					activity.isDefault()
			);
		}
	}

	public record LionProjectResponse(
			Long id,
			String name,
			LionEventType eventType,
			Integer generation,
			String myRole,
			String description,
			String link
	) {

		static LionProjectResponse from(LionProject project) {
			return new LionProjectResponse(
					project.getId(),
					project.getName(),
					project.getEventType(),
					project.getGeneration(),
					project.getMyRole(),
					project.getDescription(),
					project.getLink()
			);
		}
	}

	public record ProjectResponse(
			Long id,
			String name,
			YearMonth startYm,
			YearMonth endYm,
			boolean isCurrent,
			String role,
			String description,
			String link,
			List<String> tags
	) {

		static ProjectResponse from(Project project) {
			return new ProjectResponse(
					project.getId(),
					project.getName(),
					project.getStartYm(),
					project.getEndYm(),
					project.isCurrent(),
					project.getRole(),
					project.getDescription(),
					project.getLink(),
					Objects.requireNonNullElse(project.getTags(), List.of())
			);
		}
	}

	public record CertificationResponse(Long id, String name, String issuer, YearMonth acquiredYm) {

		static CertificationResponse from(Certification certification) {
			return new CertificationResponse(
					certification.getId(),
					certification.getName(),
					certification.getOrganization(),
					certification.getDate()
			);
		}
	}

	public record AwardResponse(Long id, String name, String host, YearMonth awardedYm, String description) {

		static AwardResponse from(Award award) {
			return new AwardResponse(
					award.getId(),
					award.getName(),
					award.getOrganization(),
					award.getDate(),
					award.getDescription()
			);
		}
	}

	public record ActivityResponse(
			Long id,
			String name,
			String organization,
			YearMonth startYm,
			YearMonth endYm,
			String description
	) {

		static ActivityResponse from(Activity activity) {
			return new ActivityResponse(
					activity.getId(),
					activity.getName(),
					activity.getOrganization(),
					activity.getStartYm(),
					activity.getEndYm(),
					activity.getDescription()
			);
		}
	}

	public record LanguageResponse(Long id, String language, LanguageLevel level) {

		static LanguageResponse from(Language language) {
			return new LanguageResponse(language.getId(), language.getLanguage(), language.getLevel());
		}
	}
}
