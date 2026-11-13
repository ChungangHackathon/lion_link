package com.lionlink.backend.domain.coffeechat.repository;

import com.lionlink.backend.domain.coffeechat.entity.CoffeeChat;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface CoffeeChatRepository extends JpaRepository<CoffeeChat, Long> {

	@Query("""
			select count(c) > 0
			from CoffeeChat c
			where c.openFlag = 1
			  and ((c.requesterId = :userId and c.receiverId = :otherUserId)
			    or (c.requesterId = :otherUserId and c.receiverId = :userId))
			""")
	boolean existsOpenChat(Long userId, Long otherUserId);

	@Query("""
			select count(c) > 0
			from CoffeeChat c
			where c.openFlag = 1
			  and ((c.requesterId in :userIds and c.receiverId = :receiverId)
			    or (c.requesterId = :receiverId and c.receiverId in :userIds)
			    or (c.receiverId = :receiverId and exists (
			          select 1 from CoffeeChatMember m
			          where m.coffeeChat = c and m.userId in :userIds and m.leftAt is null)))
			""")
	boolean existsOpenChatWithAny(Long receiverId, Collection<Long> userIds);

	@Query("""
			select max(case when c.status = :rejected then c.rejectedAt else c.expiresAt end)
			from CoffeeChat c
			where c.requesterId = :requesterId and c.receiverId = :receiverId and c.status in (:rejected, :expired)
			""")
	LocalDateTime findLastClosedAt(Long requesterId, Long receiverId, CoffeeChatStatus rejected, CoffeeChatStatus expired);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from CoffeeChat c where c.id = :id")
	Optional<CoffeeChat> findByIdForUpdate(Long id);

	@Query("select c.id from CoffeeChat c where c.status = :status and c.expiresAt <= :time")
	List<Long> findIdsToExpire(CoffeeChatStatus status, LocalDateTime time);

	@Query("select c.id from CoffeeChat c where c.status = :status and c.scheduledAt <= :time")
	List<Long> findIdsScheduledBefore(CoffeeChatStatus status, LocalDateTime time);

	@Query("select c.id from CoffeeChat c where c.status = :status and c.acceptedAt <= :time")
	List<Long> findIdsAcceptedBefore(CoffeeChatStatus status, LocalDateTime time);
}
