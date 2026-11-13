package com.lionlink.backend.domain.profile.basic.service;

import com.lionlink.backend.domain.profile.basic.dto.response.ProfileResponse;
import com.lionlink.backend.domain.profile.basic.dto.response.ProfileResponse.Sections;
import com.lionlink.backend.domain.profile.basic.entity.Profile;
import com.lionlink.backend.domain.profile.basic.repository.ProfileRepository;
import com.lionlink.backend.domain.profile.section.activity.ActivityRepository;
import com.lionlink.backend.domain.profile.section.award.AwardRepository;
import com.lionlink.backend.domain.profile.section.career.CareerRepository;
import com.lionlink.backend.domain.profile.section.certification.CertificationRepository;
import com.lionlink.backend.domain.profile.section.education.EducationRepository;
import com.lionlink.backend.domain.profile.section.language.LanguageRepository;
import com.lionlink.backend.domain.profile.section.lionactivity.LionActivityRepository;
import com.lionlink.backend.domain.profile.section.lionproject.LionProjectRepository;
import com.lionlink.backend.domain.profile.section.project.ProjectRepository;
import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyProfileService {

	private final UserRepository userRepository;
	private final ProfileRepository profileRepository;
	private final CareerRepository careerRepository;
	private final EducationRepository educationRepository;
	private final LionActivityRepository lionActivityRepository;
	private final LionProjectRepository lionProjectRepository;
	private final ProjectRepository projectRepository;
	private final CertificationRepository certificationRepository;
	private final AwardRepository awardRepository;
	private final ActivityRepository activityRepository;
	private final LanguageRepository languageRepository;

	/** PROFILE-02: 공개 범위를 적용하지 않은 내 프로필. 섹션은 저장소 쿼리가 정렬해서 준다. */
	public ProfileResponse getMyProfile(Long userId) {
		User user = userRepository.findById(userId)
				.filter(found -> found.getDeletedAt() == null)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHENTICATED));
		Profile profile = profileRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.PROFILE_NOT_COMPLETED));

		Sections sections = new Sections(
				careerRepository.findByUserIdOrderByIsCurrentDescStartYmDescIdDesc(userId),
				educationRepository.findByUserIdOrderByIsCurrentDescStartYmDescIdDesc(userId),
				lionActivityRepository.findByUserIdOrderByGenerationDesc(userId),
				lionProjectRepository.findByUserIdOrderByGenerationDescIdDesc(userId),
				projectRepository.findByUserIdOrderByIsCurrentDescStartYmDescIdDesc(userId),
				certificationRepository.findByUserIdOrderByDateDescIdDesc(userId),
				awardRepository.findByUserIdOrderByDateDescIdDesc(userId),
				activityRepository.findByUserIdOrderByStartYmDescIdDesc(userId),
				languageRepository.findByUserIdOrderByIdAsc(userId));
		return ProfileResponse.of(user, profile, sections);
	}
}
