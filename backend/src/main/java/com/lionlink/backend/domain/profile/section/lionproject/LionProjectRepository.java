package com.lionlink.backend.domain.profile.section.lionproject;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LionProjectRepository extends JpaRepository<LionProject, Long> {

	List<LionProject> findByUserIdOrderByGenerationDescIdDesc(Long userId);
}
