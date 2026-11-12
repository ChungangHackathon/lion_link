package com.lionlink.backend.domain.profile.section.activity;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

	List<Activity> findByUserIdOrderByStartYmDescIdDesc(Long userId);
}
