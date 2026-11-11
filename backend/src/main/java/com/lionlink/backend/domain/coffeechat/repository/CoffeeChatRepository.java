package com.lionlink.backend.domain.coffeechat.repository;

import com.lionlink.backend.domain.coffeechat.entity.CoffeeChat;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatStatus;
import java.util.Collection;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CoffeeChatRepository extends JpaRepository<CoffeeChat, Long> {

	@Query("""
			select case when c.requesterId = :userId then c.receiverId else c.requesterId end
			from CoffeeChat c
			where c.status in :statuses
			  and ((c.requesterId = :userId and c.receiverId in :otherUserIds)
			    or (c.receiverId = :userId and c.requesterId in :otherUserIds))
			""")
	Set<Long> findPartnerIds(Long userId, Collection<Long> otherUserIds, Collection<CoffeeChatStatus> statuses);

	@Query("""
			select count(c) > 0
			from CoffeeChat c
			where c.openFlag = 1
			  and ((c.requesterId = :userId and c.receiverId = :otherUserId)
			    or (c.requesterId = :otherUserId and c.receiverId = :userId))
			""")
	boolean existsOpenChat(Long userId, Long otherUserId);
}
