package com.lionlink.backend.domain.profile.section.career;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareerRepository extends JpaRepository<Career, Long> {

	List<Career> findByUserIdOrderByIsCurrentDescStartYmDescIdDesc(Long userId);

	List<Career> findByUserIdInAndIsCurrentTrueOrderByStartYmDescIdDesc(Collection<Long> userIds);
}
