package com.lionlink.backend.domain.profile.section.award;

import com.lionlink.backend.domain.profile.section.SectionItemRepository;
import java.util.List;

public interface AwardRepository extends SectionItemRepository<Award> {

	List<Award> findByUserIdOrderByDateDescIdDesc(Long userId);
}
