package com.lionlink.backend.domain.profile.section.certification;

import com.lionlink.backend.domain.profile.section.SectionItemRepository;
import java.util.List;

public interface CertificationRepository extends SectionItemRepository<Certification> {

	List<Certification> findByUserIdOrderByDateDescIdDesc(Long userId);
}
