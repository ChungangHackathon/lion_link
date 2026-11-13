package com.lionlink.backend.domain.profile.section.project;

import com.lionlink.backend.domain.profile.section.SectionItemRepository;
import java.util.List;

public interface ProjectRepository extends SectionItemRepository<Project> {

	List<Project> findByUserIdOrderByIsCurrentDescStartYmDescIdDesc(Long userId);
}
