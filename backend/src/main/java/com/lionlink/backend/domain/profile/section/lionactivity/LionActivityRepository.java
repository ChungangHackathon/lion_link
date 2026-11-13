package com.lionlink.backend.domain.profile.section.lionactivity;

import com.lionlink.backend.domain.profile.section.SectionItemRepository;
import java.util.List;
import java.util.Optional;

public interface LionActivityRepository extends SectionItemRepository<LionActivity> {

	List<LionActivity> findByUserIdOrderByGenerationDesc(Long userId);

	Optional<LionActivity> findByUserIdAndIsDefaultTrue(Long userId);

	boolean existsByUserIdAndGeneration(Long userId, Integer generation);

	/** PUT에서 자기 자신을 뺀 기수 중복 검사. */
	boolean existsByUserIdAndGenerationAndIdNot(Long userId, Integer generation, Long id);
}
