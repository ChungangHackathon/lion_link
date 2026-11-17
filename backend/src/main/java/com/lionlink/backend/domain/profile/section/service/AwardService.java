package com.lionlink.backend.domain.profile.section.service;

import static com.lionlink.backend.domain.profile.section.service.SectionItemSupport.blankToNull;
import static com.lionlink.backend.domain.profile.section.service.SectionItemSupport.strip;

import com.lionlink.backend.domain.profile.basic.dto.response.ProfileResponse.AwardItem;
import com.lionlink.backend.domain.profile.section.award.Award;
import com.lionlink.backend.domain.profile.section.award.AwardRepository;
import com.lionlink.backend.domain.profile.section.dto.request.AwardRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PROFILE-11 수상. 엔티티의 organization·date를 API의 host·awardedYm으로 매핑한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AwardService {

	static final int MAX_ITEMS = 10;

	private final AwardRepository awardRepository;
	private final SectionItemSupport support;

	@Transactional
	public AwardItem create(Long userId, AwardRequest request) {
		support.lockProfile(userId);
		support.checkLimit(awardRepository, userId, MAX_ITEMS);
		Award award = awardRepository.save(Award.builder()
				.userId(userId)
				.name(strip(request.name()))
				.organization(blankToNull(request.host()))
				.date(request.awardedYm())
				.description(blankToNull(request.description()))
				.build());
		return AwardItem.from(award);
	}

	@Transactional
	public AwardItem update(Long userId, Long itemId, AwardRequest request) {
		support.lockProfile(userId);
		Award award = support.findOwned(awardRepository, itemId, userId);
		award.update(strip(request.name()), blankToNull(request.host()), request.awardedYm(),
				blankToNull(request.description()));
		return AwardItem.from(award);
	}

	@Transactional
	public void delete(Long userId, Long itemId) {
		support.lockProfile(userId);
		awardRepository.delete(support.findOwned(awardRepository, itemId, userId));
	}
}
