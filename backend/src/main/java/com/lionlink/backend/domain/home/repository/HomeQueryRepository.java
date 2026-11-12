package com.lionlink.backend.domain.home.repository;

import com.lionlink.backend.domain.chat.entity.ChatRoomStatus;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChat;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatStatus;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class HomeQueryRepository {

	private static final List<CoffeeChatStatus> ACTIVE_COFFEE_CHAT = List.of(CoffeeChatStatus.ACCEPTED, CoffeeChatStatus.SCHEDULED);

	private static final String MY_COFFEE_CHAT = """
			(c.requesterId = :userId or c.receiverId = :userId
			  or exists (select 1 from CoffeeChatMember m where m.coffeeChat = c and m.userId = :userId and m.leftAt is null))
			""";

	private final EntityManager entityManager;

	public long countPendingCoffeeChats(Long userId) {
		return entityManager.createQuery(
						"select count(c) from CoffeeChat c where c.receiverId = :userId and c.status = :status", Long.class)
				.setParameter("userId", userId)
				.setParameter("status", CoffeeChatStatus.PENDING)
				.getSingleResult();
	}

	public long countActiveCoffeeChats(Long userId) {
		return entityManager.createQuery(
						"select count(c) from CoffeeChat c where c.status in :statuses and " + MY_COFFEE_CHAT, Long.class)
				.setParameter("userId", userId)
				.setParameter("statuses", ACTIVE_COFFEE_CHAT)
				.getSingleResult();
	}

	public Optional<CoffeeChat> findUpcomingCoffeeChat(Long userId, LocalDateTime now) {
		return entityManager.createQuery("""
						select c from CoffeeChat c
						where c.status = :status and c.scheduledAt >= :now and %s
						order by c.scheduledAt asc, c.id asc
						""".formatted(MY_COFFEE_CHAT), CoffeeChat.class)
				.setParameter("userId", userId)
				.setParameter("status", CoffeeChatStatus.SCHEDULED)
				.setParameter("now", now)
				.setMaxResults(1)
				.getResultStream()
				.findFirst();
	}

	public Optional<Long> findCoffeeChatRoomId(Long coffeeChatId) {
		return entityManager.createQuery("select r.id from ChatRoom r where r.coffeeChatId = :coffeeChatId", Long.class)
				.setParameter("coffeeChatId", coffeeChatId)
				.getResultStream()
				.findFirst();
	}

	public long countUnreadMessages(Long userId) {
		return entityManager.createQuery("""
						select count(m) from ChatMessage m, ChatRoomMember rm
						where rm.room = m.room and rm.userId = :userId and rm.leftAt is null
						  and m.room.status = :status and m.senderId <> :userId and m.deletedAt is null
						  and m.id > coalesce(rm.lastReadMessageId, 0) and m.id > coalesce(rm.clearedMessageId, 0)
						""", Long.class)
				.setParameter("userId", userId)
				.setParameter("status", ChatRoomStatus.ACTIVE)
				.getSingleResult();
	}

	public long countMessageRequests(Long userId) {
		return entityManager.createQuery("""
						select count(r) from ChatRoom r
						where r.status = :status and r.requestedBy <> :userId and (r.userAId = :userId or r.userBId = :userId)
						""", Long.class)
				.setParameter("userId", userId)
				.setParameter("status", ChatRoomStatus.REQUESTED)
				.getSingleResult();
	}
}
