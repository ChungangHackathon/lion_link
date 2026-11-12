package com.lionlink.backend.domain.coffeechat.repository;

import com.lionlink.backend.domain.coffeechat.entity.CoffeeChat;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
