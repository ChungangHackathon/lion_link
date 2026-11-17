package com.lionlink.backend.domain.profile.basic.repository;

import com.lionlink.backend.domain.profile.basic.entity.Profile;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

	/** 목록형 섹션 변경(PROFILE-11)을 회원별로 한 줄로 세울 때 쓴다. 트랜잭션 안에서만 호출. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Profile p where p.userId = :userId")
	Optional<Profile> findByIdForUpdate(@Param("userId") Long userId);
}
