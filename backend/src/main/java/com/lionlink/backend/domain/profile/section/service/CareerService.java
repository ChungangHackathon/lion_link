package com.lionlink.backend.domain.profile.section.service;

import static com.lionlink.backend.domain.profile.section.service.SectionItemSupport.blankToNull;
import static com.lionlink.backend.domain.profile.section.service.SectionItemSupport.strip;

import com.lionlink.backend.domain.profile.basic.dto.response.ProfileResponse.CareerItem;
import com.lionlink.backend.domain.profile.section.career.Career;
import com.lionlink.backend.domain.profile.section.career.CareerRepository;
import com.lionlink.backend.domain.profile.section.dto.request.CareerRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PROFILE-11 경력. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CareerService {

	static final int MAX_ITEMS = 10;

	private final CareerRepository careerRepository;
	private final SectionItemSupport support;

	@Transactional
	public CareerItem create(Long userId, CareerRequest request) {
		support.lockProfile(userId);
		support.checkLimit(careerRepository, userId, MAX_ITEMS);
		Career career = careerRepository.save(Career.builder()
				.userId(userId)
				.company(strip(request.company()))
				.title(strip(request.jobTitle()))
				.employmentType(request.employmentType())
				.startYm(request.startYm())
				.endYm(request.endYm())
				.isCurrent(Boolean.TRUE.equals(request.isCurrent()))
				.description(blankToNull(request.description()))
				.build());
		return CareerItem.from(career);
	}

	@Transactional
	public CareerItem update(Long userId, Long itemId, CareerRequest request) {
		support.lockProfile(userId);
		Career career = support.findOwned(careerRepository, itemId, userId);
		career.update(strip(request.company()), strip(request.jobTitle()), request.employmentType(),
				request.startYm(), request.endYm(), Boolean.TRUE.equals(request.isCurrent()),
				blankToNull(request.description()));
		return CareerItem.from(career);
	}

	@Transactional
	public void delete(Long userId, Long itemId) {
		support.lockProfile(userId);
		careerRepository.delete(support.findOwned(careerRepository, itemId, userId));
	}
}
