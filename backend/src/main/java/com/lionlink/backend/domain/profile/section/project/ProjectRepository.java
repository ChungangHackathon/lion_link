package com.lionlink.backend.domain.profile.section.project;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

	List<Project> findByUserIdOrderByIsCurrentDescStartYmDescIdDesc(Long userId);
}
