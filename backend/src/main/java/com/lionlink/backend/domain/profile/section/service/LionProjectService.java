package com.lionlink.backend.domain.profile.section.service;

import static com.lionlink.backend.domain.profile.section.service.SectionItemSupport.blankToNull;
import static com.lionlink.backend.domain.profile.section.service.SectionItemSupport.strip;

import com.lionlink.backend.domain.profile.basic.dto.response.ProfileResponse.LionProjectItem;
import com.lionlink.backend.domain.profile.section.dto.request.LionProjectRequest;
import com.lionlink.backend.domain.profile.section.lionproject.LionProject;
import com.lionlink.backend.domain.profile.section.lionproject.LionProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PROFILE-11 멋사 프로젝트·해커톤. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LionProjectService {

	static final int MAX_ITEMS = 5;

	private final LionProjectRepository lionProjectRepository;
	private final SectionItemSupport support;

	@Transactional
	public LionProjectItem create(Long userId, LionProjectRequest request) {
		support.lockProfile(userId);
		support.checkLimit(lionProjectRepository, userId, MAX_ITEMS);
		LionProject project = lionProjectRepository.save(LionProject.builder()
				.userId(userId)
				.name(strip(request.name()))
				.eventType(request.eventType())
				.generation(request.generation())
				.myRole(blankToNull(request.myRole()))
				.description(blankToNull(request.description()))
				.link(blankToNull(request.link()))
				.build());
		return LionProjectItem.from(project);
	}

	@Transactional
	public LionProjectItem update(Long userId, Long itemId, LionProjectRequest request) {
		support.lockProfile(userId);
		LionProject project = support.findOwned(lionProjectRepository, itemId, userId);
		project.update(strip(request.name()), request.eventType(), request.generation(),
				blankToNull(request.myRole()), blankToNull(request.description()), blankToNull(request.link()));
		return LionProjectItem.from(project);
	}

	@Transactional
	public void delete(Long userId, Long itemId) {
		support.lockProfile(userId);
		lionProjectRepository.delete(support.findOwned(lionProjectRepository, itemId, userId));
	}
}
