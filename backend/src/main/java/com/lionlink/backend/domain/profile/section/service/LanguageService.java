package com.lionlink.backend.domain.profile.section.service;

import static com.lionlink.backend.domain.profile.section.service.SectionItemSupport.strip;

import com.lionlink.backend.domain.profile.basic.dto.response.ProfileResponse.LanguageItem;
import com.lionlink.backend.domain.profile.exception.ProfileErrorCode;
import com.lionlink.backend.domain.profile.section.dto.request.LanguageRequest;
import com.lionlink.backend.domain.profile.section.language.Language;
import com.lionlink.backend.domain.profile.section.language.LanguageRepository;
import com.lionlink.backend.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PROFILE-11 언어. 같은 언어는 대소문자를 무시하고 한 번만 등록할 수 있다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LanguageService {

	static final int MAX_ITEMS = 5;

	/** 새 항목에는 id가 없으니, 어떤 항목과도 겹치지 않는 값으로 자기 자신 제외 조건을 채운다. */
	private static final long NO_ITEM = -1L;

	private final LanguageRepository languageRepository;
	private final SectionItemSupport support;

	@Transactional
	public LanguageItem create(Long userId, LanguageRequest request) {
		support.lockProfile(userId);
		support.checkLimit(languageRepository, userId, MAX_ITEMS);
		String name = strip(request.language());
		checkDuplicate(userId, name, NO_ITEM);
		Language language = languageRepository.save(Language.builder()
				.userId(userId)
				.language(name)
				.level(request.level())
				.build());
		return LanguageItem.from(language);
	}

	@Transactional
	public LanguageItem update(Long userId, Long itemId, LanguageRequest request) {
		support.lockProfile(userId);
		Language language = support.findOwned(languageRepository, itemId, userId);
		String name = strip(request.language());
		checkDuplicate(userId, name, itemId);
		language.update(name, request.level());
		return LanguageItem.from(language);
	}

	@Transactional
	public void delete(Long userId, Long itemId) {
		support.lockProfile(userId);
		languageRepository.delete(support.findOwned(languageRepository, itemId, userId));
	}

	private void checkDuplicate(Long userId, String name, Long exceptItemId) {
		if (languageRepository.existsByUserIdAndLanguageIgnoreCaseAndIdNot(userId, name, exceptItemId)) {
			throw new BusinessException(ProfileErrorCode.DUPLICATE_ITEM);
		}
	}
}
