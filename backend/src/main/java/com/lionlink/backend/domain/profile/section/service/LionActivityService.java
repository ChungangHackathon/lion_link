package com.lionlink.backend.domain.profile.section.service;

import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.basic.dto.response.ProfileResponse.LionActivityItem;
import com.lionlink.backend.domain.profile.exception.ProfileErrorCode;
import com.lionlink.backend.domain.profile.section.dto.request.LionActivityRequest;
import com.lionlink.backend.domain.profile.section.lionactivity.LionActivity;
import com.lionlink.backend.domain.profile.section.lionactivity.LionActivityRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.CommonErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PROFILE-11 멋사 활동. 가입 기수 항목(isDefault)의 기수는 DefaultLionActivitySyncer가 관리한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LionActivityService {

	static final int MAX_ITEMS = 6;

	private final LionActivityRepository lionActivityRepository;
	private final SectionItemSupport support;

	@Transactional
	public LionActivityItem create(Long userId, LionActivityRequest request) {
		support.lockProfile(userId);
		support.checkLimit(lionActivityRepository, userId, MAX_ITEMS);
		if (lionActivityRepository.existsByUserIdAndGeneration(userId, request.generation())) {
			throw new BusinessException(ProfileErrorCode.DUPLICATE_ITEM);
		}
		LionActivity activity = lionActivityRepository.save(LionActivity.builder()
				.userId(userId)
				.generation(request.generation())
				.role(request.role())
				.part1(part(request.parts(), 0))
				.part2(part(request.parts(), 1))
				.isDefault(false)
				.build());
		return LionActivityItem.from(activity);
	}

	/** 가입 기수 항목은 role·parts만 반영하고, 다른 기수를 보내면 400. */
	@Transactional
	public LionActivityItem update(Long userId, Long itemId, LionActivityRequest request) {
		support.lockProfile(userId);
		LionActivity activity = support.findOwned(lionActivityRepository, itemId, userId);
		LionPart part1 = part(request.parts(), 0);
		LionPart part2 = part(request.parts(), 1);
		if (activity.isDefault()) {
			if (!activity.getGeneration().equals(request.generation())) {
				throw new BusinessException(CommonErrorCode.VALIDATION_ERROR, "가입 기수 항목은 기수를 바꿀 수 없어요.");
			}
			activity.updateRoleAndParts(request.role(), part1, part2);
		} else {
			if (lionActivityRepository.existsByUserIdAndGenerationAndIdNot(userId, request.generation(), itemId)) {
				throw new BusinessException(ProfileErrorCode.DUPLICATE_ITEM);
			}
			activity.update(request.generation(), request.role(), part1, part2);
		}
		return LionActivityItem.from(activity);
	}

	@Transactional
	public void delete(Long userId, Long itemId) {
		support.lockProfile(userId);
		LionActivity activity = support.findOwned(lionActivityRepository, itemId, userId);
		if (activity.isDefault()) {
			throw new BusinessException(ProfileErrorCode.DEFAULT_ITEM_NOT_DELETABLE);
		}
		lionActivityRepository.delete(activity);
	}

	private static LionPart part(List<LionPart> parts, int index) {
		return parts.size() > index ? parts.get(index) : null;
	}
}
