package com.lionlink.backend.domain.profile.section.lionactivity;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LionActivityRepository extends JpaRepository<LionActivity, Long> {

	List<LionActivity> findByUserIdOrderByGenerationDesc(Long userId);
}
