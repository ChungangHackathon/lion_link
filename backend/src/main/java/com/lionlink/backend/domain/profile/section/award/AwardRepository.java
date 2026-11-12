package com.lionlink.backend.domain.profile.section.award;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AwardRepository extends JpaRepository<Award, Long> {

	List<Award> findByUserIdOrderByDateDescIdDesc(Long userId);
}
