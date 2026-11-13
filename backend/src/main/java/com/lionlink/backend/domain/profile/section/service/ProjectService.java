package com.lionlink.backend.domain.profile.section.service;

import static com.lionlink.backend.domain.profile.section.service.SectionItemSupport.blankToNull;
import static com.lionlink.backend.domain.profile.section.service.SectionItemSupport.strip;

import com.lionlink.backend.domain.profile.basic.dto.response.ProfileResponse.ProjectItem;
import com.lionlink.backend.domain.profile.section.dto.request.ProjectRequest;
import com.lionlink.backend.domain.profile.section.project.Project;
import com.lionlink.backend.domain.profile.section.project.ProjectRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PROFILE-11 프로젝트. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

	static final int MAX_ITEMS = 10;

	private final ProjectRepository projectRepository;
	private final SectionItemSupport support;

	@Transactional
	public ProjectItem create(Long userId, ProjectRequest request) {
		support.lockProfile(userId);
		support.checkLimit(projectRepository, userId, MAX_ITEMS);
		Project project = projectRepository.save(Project.builder()
				.userId(userId)
				.name(strip(request.name()))
				.startYm(request.startYm())
				.endYm(Boolean.TRUE.equals(request.isCurrent()) ? null : request.endYm())
				.isCurrent(Boolean.TRUE.equals(request.isCurrent()))
				.role(blankToNull(request.role()))
				.description(blankToNull(request.description()))
				.link(blankToNull(request.link()))
				.tags(tags(request))
				.build());
		return ProjectItem.from(project);
	}

	@Transactional
	public ProjectItem update(Long userId, Long itemId, ProjectRequest request) {
		support.lockProfile(userId);
		Project project = support.findOwned(projectRepository, itemId, userId);
		project.update(strip(request.name()), request.startYm(), request.endYm(),
				Boolean.TRUE.equals(request.isCurrent()), blankToNull(request.role()),
				blankToNull(request.description()), blankToNull(request.link()), tags(request));
		return ProjectItem.from(project);
	}

	@Transactional
	public void delete(Long userId, Long itemId) {
		support.lockProfile(userId);
		projectRepository.delete(support.findOwned(projectRepository, itemId, userId));
	}

	/** 앞뒤 공백을 지운다. 비었으면 null로 저장. */
	private static List<String> tags(ProjectRequest request) {
		if (request.tags() == null || request.tags().isEmpty()) {
			return null;
		}
		return request.tags().stream().map(String::strip).toList();
	}
}
