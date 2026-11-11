package com.lionlink.backend.domain.alumni.dto.response;

import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.ProfileStatus;
import com.lionlink.backend.domain.profile.basic.entity.Profile;
import com.lionlink.backend.domain.profile.basic.entity.ProfileLink;
import com.lionlink.backend.domain.profile.section.career.Career;
import com.lionlink.backend.domain.profile.section.education.Degree;
import com.lionlink.backend.domain.profile.section.education.Education;
import com.lionlink.backend.domain.profile.section.lionactivity.LionActivity;
import com.lionlink.backend.domain.profile.section.lionactivity.LionRole;
import com.lionlink.backend.domain.profile.section.project.Project;
import com.lionlink.backend.domain.user.entity.User;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public record AlumniDetailResponse(
		Long id,
		String name,
		Integer generation,
		List<LionPart> lionParts,
		ProfileStatus status,
		String headline,
		String bio,
		String photoUrl,
		String major,
		String region,
		List<String> interests,
		List<String> skills,
		List<ProfileLink> links,
		List<CareerResponse> careers,
		List<EducationResponse> educations,
		List<ProjectResponse> projects,
		List<LionActivityResponse> lionActivities,
		String contactEmail,
		String phone,
		boolean bookmarked,
		boolean coffeeChatAvailable
) {

	public static AlumniDetailResponse of(User user, Profile profile, Sections sections, Viewer viewer) {
		boolean owner = viewer.owner();
		boolean connected = viewer.connected();
		boolean companyVisible = profile.getCompanyVisibility().isVisibleTo(owner, connected);
		return new AlumniDetailResponse(
				user.getId(),
				user.getName(),
				user.getGeneration(),
				profile.getLionParts(),
				profile.getStatus(),
				profile.getHeadline(),
				profile.getBio(),
				profile.getPhotoUrl(),
				profile.getMajor(),
				profile.getRegion(),
				profile.getInterests(),
				Objects.requireNonNullElse(profile.getSkills(), List.of()),
				profile.getLinksVisibility().isVisibleTo(owner, connected)
						? Objects.requireNonNullElse(profile.getLinks(), List.of())
						: null,
				sections.careers().stream().map(career -> CareerResponse.of(career, companyVisible)).toList(),
				sections.educations().stream().map(EducationResponse::from).toList(),
				sections.projects().stream().map(ProjectResponse::from).toList(),
				sections.lionActivities().stream().map(LionActivityResponse::from).toList(),
				profile.getEmailVisibility().isVisibleTo(owner, connected) ? profile.getContactEmail() : null,
				profile.getPhoneVisibility().isVisibleTo(owner, connected) ? profile.getPhone() : null,
				viewer.bookmarked(),
				viewer.coffeeChatAvailable()
		);
	}

	public record Sections(
			List<Career> careers,
			List<Education> educations,
			List<Project> projects,
			List<LionActivity> lionActivities
	) {
	}

	public record Viewer(boolean owner, boolean connected, boolean bookmarked, boolean coffeeChatAvailable) {
	}

	public record CareerResponse(
			Long id,
			String company,
			String title,
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
					career.getTitle(),
					career.getEmploymentType(),
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
}
