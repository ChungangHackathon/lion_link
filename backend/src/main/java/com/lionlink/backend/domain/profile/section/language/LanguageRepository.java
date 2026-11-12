package com.lionlink.backend.domain.profile.section.language;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LanguageRepository extends JpaRepository<Language, Long> {

	List<Language> findByUserIdOrderByIdAsc(Long userId);
}
