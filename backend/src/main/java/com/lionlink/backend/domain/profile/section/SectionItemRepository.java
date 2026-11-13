package com.lionlink.backend.domain.profile.section;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/** 목록형 섹션 저장소의 공통 조회. 남의 항목은 id만 맞아도 찾지 못하게 항상 userId를 같이 건다. */
@NoRepositoryBean
public interface SectionItemRepository<T extends ProfileSectionItem> extends JpaRepository<T, Long> {

	Optional<T> findByIdAndUserId(Long id, Long userId);

	long countByUserId(Long userId);
}
