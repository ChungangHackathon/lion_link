package com.lionlink.backend.domain.profile.section.language;

import com.lionlink.backend.domain.profile.section.SectionItemRepository;
import java.util.List;

public interface LanguageRepository extends SectionItemRepository<Language> {

	List<Language> findByUserIdOrderByIdAsc(Long userId);

	/** 대소문자를 무시한 중복 검사. PUT에서는 자기 자신을 뺀다. */
	boolean existsByUserIdAndLanguageIgnoreCaseAndIdNot(Long userId, String language, Long id);
}
