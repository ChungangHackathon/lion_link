package com.lionlink.backend.domain.profile.section.education;

import com.lionlink.backend.domain.profile.section.SectionItemRepository;
import java.util.List;

public interface EducationRepository extends SectionItemRepository<Education> {

	List<Education> findByUserIdOrderByIsCurrentDescStartYmDescIdDesc(Long userId);
}
