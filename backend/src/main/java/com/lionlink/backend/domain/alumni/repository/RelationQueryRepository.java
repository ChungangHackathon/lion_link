package com.lionlink.backend.domain.alumni.repository;

import com.lionlink.backend.domain.chat.entity.ChatRoom;
import com.lionlink.backend.domain.chat.entity.ChatRoomStatus;
import com.lionlink.backend.domain.chat.entity.ChatRoomType;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RelationQueryRepository {

	private static final String ACCEPTED_MEMBER = "c.acceptedAt is not null and (m.leftAt is null or m.leftAt > c.acceptedAt)";

	private final EntityManager entityManager;

	public Set<Long> findConnectedIds(Long userId) {
		Set<Long> ids = new HashSet<>();
		ids.addAll(partnerIds("""
				select case when c.requesterId = :userId then c.receiverId else c.requesterId end
				from CoffeeChat c
				where c.acceptedAt is not null and (c.requesterId = :userId or c.receiverId = :userId)
				""", Map.of("userId", userId)));
		ids.addAll(partnerIds("select m.userId from CoffeeChatMember m join m.coffeeChat c where c.receiverId = :userId and "
				+ ACCEPTED_MEMBER, Map.of("userId", userId)));
		ids.addAll(partnerIds("select c.receiverId from CoffeeChatMember m join m.coffeeChat c where m.userId = :userId and "
				+ ACCEPTED_MEMBER, Map.of("userId", userId)));
		ids.addAll(partnerIds("""
				select case when r.userAId = :userId then r.userBId else r.userAId end
				from ChatRoom r
				where r.type = :direct and r.status = :active and (r.userAId = :userId or r.userBId = :userId)
				""", Map.of("userId", userId, "direct", ChatRoomType.DIRECT, "active", ChatRoomStatus.ACTIVE)));
		ids.remove(userId);
		return ids;
	}

	public boolean existsInProgress(Long userId, Long targetUserId) {
		return entityManager.createQuery("""
						select count(c) > 0
						from CoffeeChat c
						where c.openFlag = 1
						  and ((c.requesterId = :userId and c.receiverId = :targetUserId)
						    or (c.requesterId = :targetUserId and c.receiverId = :userId)
						    or (c.receiverId = :targetUserId and exists (
						          select 1 from CoffeeChatMember m
						          where m.coffeeChat = c and m.userId = :userId and m.leftAt is null)))
						""", Boolean.class)
				.setParameter("userId", userId)
				.setParameter("targetUserId", targetUserId)
				.getSingleResult();
	}

	public Optional<Long> findPendingCoffeeChatId(Long userId, Long targetUserId) {
		return entityManager.createQuery("""
						select c.id from CoffeeChat c
						where c.requesterId = :userId and c.receiverId = :targetUserId and c.status = :pending
						order by c.id desc
						""", Long.class)
				.setParameter("userId", userId)
				.setParameter("targetUserId", targetUserId)
				.setParameter("pending", CoffeeChatStatus.PENDING)
				.setMaxResults(1)
				.getResultStream()
				.findFirst();
	}

	public Optional<ChatRoom> findDirectRoom(Long userId, Long targetUserId) {
		return entityManager.createQuery("""
						select r from ChatRoom r
						where r.type = :direct
						  and ((r.userAId = :userId and r.userBId = :targetUserId)
						    or (r.userAId = :targetUserId and r.userBId = :userId))
						order by r.id desc
						""", ChatRoom.class)
				.setParameter("direct", ChatRoomType.DIRECT)
				.setParameter("userId", userId)
				.setParameter("targetUserId", targetUserId)
				.setMaxResults(1)
				.getResultStream()
				.findFirst();
	}

	private Set<Long> partnerIds(String jpql, Map<String, Object> params) {
		TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
		params.forEach(query::setParameter);
		return new HashSet<>(query.getResultList());
	}
}
