package com.lionlink.backend.domain.coffeechat.repository;

import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatBox;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatFilter;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChat;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatMemberRole;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CoffeeChatQueryRepository {

	private static final Set<CoffeeChatStatus> ACTIVE = EnumSet.of(CoffeeChatStatus.ACCEPTED, CoffeeChatStatus.SCHEDULED);
	private static final Set<CoffeeChatStatus> ENDED = EnumSet.of(CoffeeChatStatus.COMPLETED, CoffeeChatStatus.REJECTED,
			CoffeeChatStatus.CANCELED, CoffeeChatStatus.EXPIRED);
	private static final String ACTIVE_MEMBER = """
			exists (select 1 from CoffeeChatMember m
			        where m.coffeeChat = c and m.userId = :userId and m.role = :memberRole and m.leftAt is null)""";

	private final EntityManager entityManager;

	public Slice<CoffeeChat> findMine(Long userId, CoffeeChatBox box, CoffeeChatFilter filter, Pageable pageable) {
		String boxCondition = box == CoffeeChatBox.RECEIVED
				? "c.receiverId = :userId"
				: "(c.requesterId = :userId or " + ACTIVE_MEMBER + ")";
		String statusCondition = filter == CoffeeChatFilter.ALL ? "" : " and c.status in :statuses";
		TypedQuery<CoffeeChat> query = entityManager.createQuery(
						"select c from CoffeeChat c where " + boxCondition + statusCondition + " order by c.createdAt desc, c.id desc",
						CoffeeChat.class)
				.setParameter("userId", userId)
				.setFirstResult((int) pageable.getOffset())
				.setMaxResults(pageable.getPageSize() + 1);
		if (box == CoffeeChatBox.SENT) {
			query.setParameter("memberRole", CoffeeChatMemberRole.MEMBER);
		}
		if (filter != CoffeeChatFilter.ALL) {
			query.setParameter("statuses", statuses(filter));
		}
		List<CoffeeChat> rows = query.getResultList();
		boolean hasNext = rows.size() > pageable.getPageSize();
		return new SliceImpl<>(hasNext ? rows.subList(0, pageable.getPageSize()) : rows, pageable, hasNext);
	}

	public long countPendingReceived(Long userId) {
		return entityManager.createQuery(
						"select count(c) from CoffeeChat c where c.receiverId = :userId and c.status = :pending", Long.class)
				.setParameter("userId", userId)
				.setParameter("pending", CoffeeChatStatus.PENDING)
				.getSingleResult();
	}

	public long countActive(Long userId) {
		return entityManager.createQuery("select count(c) from CoffeeChat c where c.status in :statuses"
						+ " and (c.receiverId = :userId or c.requesterId = :userId or " + ACTIVE_MEMBER + ")", Long.class)
				.setParameter("statuses", ACTIVE)
				.setParameter("userId", userId)
				.setParameter("memberRole", CoffeeChatMemberRole.MEMBER)
				.getSingleResult();
	}

	private Set<CoffeeChatStatus> statuses(CoffeeChatFilter filter) {
		return switch (filter) {
			case PENDING -> EnumSet.of(CoffeeChatStatus.PENDING);
			case ACTIVE -> ACTIVE;
			case ENDED -> ENDED;
			case ALL -> EnumSet.allOf(CoffeeChatStatus.class);
		};
	}
}
