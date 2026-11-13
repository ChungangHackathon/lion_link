package com.lionlink.backend.domain.community.recruit.repository;

import com.lionlink.backend.domain.community.recruit.entity.CoffeeChatRecruit;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface CoffeeChatRecruitRepository extends JpaRepository<CoffeeChatRecruit, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from CoffeeChatRecruit r where r.postId = :postId")
	Optional<CoffeeChatRecruit> findByIdForUpdate(Long postId);

	Optional<CoffeeChatRecruit> findByCoffeeChatId(Long coffeeChatId);
}
