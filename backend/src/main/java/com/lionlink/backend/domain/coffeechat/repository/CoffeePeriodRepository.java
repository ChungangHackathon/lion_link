package com.lionlink.backend.domain.coffeechat.repository;

import com.lionlink.backend.domain.coffeechat.entity.CoffeePeriod;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoffeePeriodRepository extends JpaRepository<CoffeePeriod, Long> {

	List<CoffeePeriod> findByUserIdInAndEndDateGreaterThanEqual(Collection<Long> userIds, LocalDate date);

	/** 지난 기간을 뺀 내 기간. 시작일이 빠른 순. */
	List<CoffeePeriod> findByUserIdAndEndDateGreaterThanEqualOrderByStartDateAsc(Long userId, LocalDate date);

	Optional<CoffeePeriod> findByIdAndUserId(Long id, Long userId);

	void deleteByUserId(Long userId);
}
