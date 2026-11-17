package com.lionlink.backend.domain.profile.section.activity;

import com.lionlink.backend.domain.profile.section.SectionItemRepository;
import java.util.List;

public interface ActivityRepository extends SectionItemRepository<Activity> {

	List<Activity> findByUserIdOrderByStartYmDescIdDesc(Long userId);
}
