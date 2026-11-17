package com.lionlink.backend.domain.profile.section.service;

import static com.lionlink.backend.domain.profile.section.service.SectionItemSupport.blankToNull;
import static com.lionlink.backend.domain.profile.section.service.SectionItemSupport.strip;

import com.lionlink.backend.domain.profile.basic.dto.response.ProfileResponse.ActivityItem;
import com.lionlink.backend.domain.profile.section.activity.Activity;
import com.lionlink.backend.domain.profile.section.activity.ActivityRepository;
import com.lionlink.backend.domain.profile.section.dto.request.ActivityRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PROFILE-11 대외활동. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityService {

	static final int MAX_ITEMS = 10;

	private final ActivityRepository activityRepository;
	private final SectionItemSupport support;

	@Transactional
	public ActivityItem create(Long userId, ActivityRequest request) {
		support.lockProfile(userId);
		support.checkLimit(activityRepository, userId, MAX_ITEMS);
		Activity activity = activityRepository.save(Activity.builder()
				.userId(userId)
				.name(strip(request.name()))
				.organization(blankToNull(request.organization()))
				.startYm(request.startYm())
				.endYm(request.endYm())
				.description(blankToNull(request.description()))
				.build());
		return ActivityItem.from(activity);
	}

	@Transactional
	public ActivityItem update(Long userId, Long itemId, ActivityRequest request) {
		support.lockProfile(userId);
		Activity activity = support.findOwned(activityRepository, itemId, userId);
		activity.update(strip(request.name()), blankToNull(request.organization()), request.startYm(),
				request.endYm(), blankToNull(request.description()));
		return ActivityItem.from(activity);
	}

	@Transactional
	public void delete(Long userId, Long itemId) {
		support.lockProfile(userId);
		activityRepository.delete(support.findOwned(activityRepository, itemId, userId));
	}
}
