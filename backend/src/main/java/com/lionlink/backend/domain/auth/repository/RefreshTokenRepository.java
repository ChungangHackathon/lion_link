package com.lionlink.backend.domain.auth.repository;

import com.lionlink.backend.domain.auth.entity.RefreshToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	/** 회원의 살아 있는 토큰을 전부 폐기한다. 폐기된 토큰 재사용(탈취 의심)·탈퇴 때 쓴다. clear 전에 flush해야 같은 트랜잭션에서 바꾼 엔티티(탈퇴 표시)가 사라지지 않는다. */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update RefreshToken t set t.revokedAt = :now where t.userId = :userId and t.revokedAt is null")
	int revokeAllByUserId(Long userId, LocalDateTime now);
}
