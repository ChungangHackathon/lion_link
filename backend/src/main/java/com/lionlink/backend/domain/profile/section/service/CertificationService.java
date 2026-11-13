package com.lionlink.backend.domain.profile.section.service;

import static com.lionlink.backend.domain.profile.section.service.SectionItemSupport.blankToNull;
import static com.lionlink.backend.domain.profile.section.service.SectionItemSupport.strip;

import com.lionlink.backend.domain.profile.basic.dto.response.ProfileResponse.CertificationItem;
import com.lionlink.backend.domain.profile.section.certification.Certification;
import com.lionlink.backend.domain.profile.section.certification.CertificationRepository;
import com.lionlink.backend.domain.profile.section.dto.request.CertificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PROFILE-11 자격증. 엔티티의 organization·date를 API의 issuer·acquiredYm으로 매핑한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CertificationService {

	static final int MAX_ITEMS = 10;

	private final CertificationRepository certificationRepository;
	private final SectionItemSupport support;

	@Transactional
	public CertificationItem create(Long userId, CertificationRequest request) {
		support.lockProfile(userId);
		support.checkLimit(certificationRepository, userId, MAX_ITEMS);
		Certification certification = certificationRepository.save(Certification.builder()
				.userId(userId)
				.name(strip(request.name()))
				.organization(blankToNull(request.issuer()))
				.date(request.acquiredYm())
				.build());
		return CertificationItem.from(certification);
	}

	@Transactional
	public CertificationItem update(Long userId, Long itemId, CertificationRequest request) {
		support.lockProfile(userId);
		Certification certification = support.findOwned(certificationRepository, itemId, userId);
		certification.update(strip(request.name()), blankToNull(request.issuer()), request.acquiredYm());
		return CertificationItem.from(certification);
	}

	@Transactional
	public void delete(Long userId, Long itemId) {
		support.lockProfile(userId);
		certificationRepository.delete(support.findOwned(certificationRepository, itemId, userId));
	}
}
