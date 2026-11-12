package com.lionlink.backend.domain.profile.section.lionactivity;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LionActivityRepository extends JpaRepository<LionActivity, Long> {

	List<LionActivity> findByUserIdOrderByGenerationDesc(Long userId);

	Optional<LionActivity> findByUserIdAndIsDefaultTrue(Long userId);

	boolean existsByUserIdAndGeneration(Long userId, Integer generation);
}
