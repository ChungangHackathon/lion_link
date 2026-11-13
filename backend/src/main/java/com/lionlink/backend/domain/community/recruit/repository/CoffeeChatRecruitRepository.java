package com.lionlink.backend.domain.community.recruit.repository;

import com.lionlink.backend.domain.community.recruit.entity.CoffeeChatRecruit;
import com.lionlink.backend.domain.community.recruit.entity.RecruitStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface CoffeeChatRecruitRepository extends JpaRepository<CoffeeChatRecruit, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from CoffeeChatRecruit r where r.postId = :postId")
	Optional<CoffeeChatRecruit> findByIdForUpdate(Long postId);

	Optional<CoffeeChatRecruit> findByCoffeeChatId(Long coffeeChatId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select r from CoffeeChatRecruit r join fetch r.post p
			where r.seniorId = :seniorId and r.status = :status and r.deadline >= :today and p.deletedAt is null
			order by r.postId
			""")
	List<CoffeeChatRecruit> findBySeniorForUpdate(Long seniorId, RecruitStatus status, LocalDate today);
}
