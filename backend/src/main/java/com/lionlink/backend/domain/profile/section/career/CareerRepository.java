package com.lionlink.backend.domain.profile.section.career;

import com.lionlink.backend.domain.profile.section.SectionItemRepository;
import java.util.Collection;
import java.util.List;

public interface CareerRepository extends SectionItemRepository<Career> {

	List<Career> findByUserIdOrderByIsCurrentDescStartYmDescIdDesc(Long userId);

	List<Career> findByUserIdInAndIsCurrentTrueOrderByStartYmDescIdDesc(Collection<Long> userIds);
}
