package com.lionlink.backend.domain.profile.section.lionproject;

import com.lionlink.backend.domain.profile.section.SectionItemRepository;
import java.util.List;

public interface LionProjectRepository extends SectionItemRepository<LionProject> {

	List<LionProject> findByUserIdOrderByGenerationDescIdDesc(Long userId);
}
