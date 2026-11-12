package com.lionlink.backend.domain.coffeechat.repository;

import com.lionlink.backend.domain.coffeechat.entity.CoffeePeriod;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoffeePeriodRepository extends JpaRepository<CoffeePeriod, Long> {

	List<CoffeePeriod> findByUserIdInAndEndDateGreaterThanEqual(Collection<Long> userIds, LocalDate date);
}
