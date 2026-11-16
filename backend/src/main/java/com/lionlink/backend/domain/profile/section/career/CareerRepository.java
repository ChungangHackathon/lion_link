package com.lionlink.backend.domain.profile.section.career;

import com.lionlink.backend.domain.profile.section.SectionItemRepository;
import java.util.List;

public interface CareerRepository extends SectionItemRepository<Career> {

	List<Career> findByUserIdOrderByStartYmDescIdDesc(Long userId);
}
