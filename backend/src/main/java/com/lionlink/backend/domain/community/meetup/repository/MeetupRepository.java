package com.lionlink.backend.domain.community.meetup.repository;

import com.lionlink.backend.domain.community.meetup.entity.Meetup;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface MeetupRepository extends JpaRepository<Meetup, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select m from Meetup m where m.postId = :postId")
	Optional<Meetup> findByIdForUpdate(Long postId);

	@Query("""
			select m from Meetup m join fetch m.post p
			where p.authorId = :userId and p.deletedAt is null and m.canceledAt is null and m.meetAt > :now
			""")
	List<Meetup> findHostedInProgress(Long userId, LocalDateTime now);
}
