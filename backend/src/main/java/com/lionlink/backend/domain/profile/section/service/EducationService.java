package com.lionlink.backend.domain.profile.section.service;

import static com.lionlink.backend.domain.profile.section.service.SectionItemSupport.blankToNull;
import static com.lionlink.backend.domain.profile.section.service.SectionItemSupport.strip;

import com.lionlink.backend.domain.profile.basic.dto.response.ProfileResponse.EducationItem;
import com.lionlink.backend.domain.profile.section.dto.request.EducationRequest;
import com.lionlink.backend.domain.profile.section.education.Education;
import com.lionlink.backend.domain.profile.section.education.EducationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PROFILE-11 학력. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EducationService {

	static final int MAX_ITEMS = 5;

	private final EducationRepository educationRepository;
	private final SectionItemSupport support;

	@Transactional
	public EducationItem create(Long userId, EducationRequest request) {
		support.lockProfile(userId);
		support.checkLimit(educationRepository, userId, MAX_ITEMS);
		Education education = educationRepository.save(Education.builder()
				.userId(userId)
				.school(strip(request.school()))
				.major(blankToNull(request.major()))
				.degree(request.degree())
				.startYm(request.startYm())
				.endYm(Boolean.TRUE.equals(request.isCurrent()) ? null : request.endYm())
				.isCurrent(Boolean.TRUE.equals(request.isCurrent()))
				.description(blankToNull(request.description()))
				.build());
		return EducationItem.from(education);
	}

	@Transactional
	public EducationItem update(Long userId, Long itemId, EducationRequest request) {
		support.lockProfile(userId);
		Education education = support.findOwned(educationRepository, itemId, userId);
		education.update(strip(request.school()), blankToNull(request.major()), request.degree(),
				request.startYm(), request.endYm(), Boolean.TRUE.equals(request.isCurrent()),
				blankToNull(request.description()));
		return EducationItem.from(education);
	}

	@Transactional
	public void delete(Long userId, Long itemId) {
		support.lockProfile(userId);
		educationRepository.delete(support.findOwned(educationRepository, itemId, userId));
	}
}
