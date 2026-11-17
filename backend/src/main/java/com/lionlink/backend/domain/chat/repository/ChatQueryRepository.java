package com.lionlink.backend.domain.chat.repository;

import com.lionlink.backend.domain.chat.entity.ChatMessage;
import com.lionlink.backend.domain.chat.entity.ChatRoomMember;
import com.lionlink.backend.domain.chat.entity.ChatRoomStatus;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ChatQueryRepository {

	private final EntityManager entityManager;

	public Slice<ChatRoomMember> findMyRooms(Long userId, Pageable pageable) {
		List<ChatRoomMember> rows = entityManager.createQuery("""
						select rm from ChatRoomMember rm join fetch rm.room r
						where rm.userId = :userId and rm.leftAt is null
						  and (r.status = :active or (r.status in :requestStatuses and r.requestedBy = :userId))
						order by case when rm.pinnedAt is null then 1 else 0 end, rm.pinnedAt desc,
						         coalesce(r.lastMessageAt, r.createdAt) desc, r.id desc
						""", ChatRoomMember.class)
				.setParameter("userId", userId)
				.setParameter("active", ChatRoomStatus.ACTIVE)
				.setParameter("requestStatuses", EnumSet.of(ChatRoomStatus.REQUESTED, ChatRoomStatus.DECLINED))
				.setFirstResult((int) pageable.getOffset())
				.setMaxResults(pageable.getPageSize() + 1)
				.getResultList();
		return slice(rows, pageable);
	}

	public Slice<ChatRoomMember> findReceivedRequests(Long userId, Pageable pageable) {
		List<ChatRoomMember> rows = entityManager.createQuery("""
						select rm from ChatRoomMember rm join fetch rm.room r
						where rm.userId = :userId and rm.leftAt is null
						  and r.status = :requested and r.requestedBy <> :userId
						order by r.requestedAt desc, r.id desc
						""", ChatRoomMember.class)
				.setParameter("userId", userId)
				.setParameter("requested", ChatRoomStatus.REQUESTED)
				.setFirstResult((int) pageable.getOffset())
				.setMaxResults(pageable.getPageSize() + 1)
				.getResultList();
		return slice(rows, pageable);
	}

	public long countUnreadMessages(Long userId) {
		return entityManager.createQuery("""
						select count(m) from ChatMessage m, ChatRoomMember rm
						where rm.room = m.room and rm.userId = :userId and rm.leftAt is null
						  and m.room.status = :active and m.senderId <> :userId and m.deletedAt is null
						  and m.id > coalesce(rm.lastReadMessageId, 0) and m.id > coalesce(rm.clearedMessageId, 0)
						""", Long.class)
				.setParameter("userId", userId)
				.setParameter("active", ChatRoomStatus.ACTIVE)
				.getSingleResult();
	}

	public long countReceivedRequests(Long userId) {
		return entityManager.createQuery("""
						select count(rm) from ChatRoomMember rm join rm.room r
						where rm.userId = :userId and rm.leftAt is null
						  and r.status = :requested and r.requestedBy <> :userId
						""", Long.class)
				.setParameter("userId", userId)
				.setParameter("requested", ChatRoomStatus.REQUESTED)
				.getSingleResult();
	}

	public Map<Long, ChatMessage> findLastMessages(Collection<Long> roomIds) {
		if (roomIds.isEmpty()) {
			return Map.of();
		}
		return entityManager.createQuery("""
						select m from ChatMessage m
						where m.id in (select max(latest.id) from ChatMessage latest where latest.room.id in :roomIds group by latest.room.id)
						""", ChatMessage.class)
				.setParameter("roomIds", roomIds)
				.getResultStream()
				.collect(Collectors.toMap(message -> message.getRoom().getId(), message -> message));
	}

	public Map<Long, Long> countUnreadByRoom(Long userId, Collection<Long> roomIds) {
		if (roomIds.isEmpty()) {
			return Map.of();
		}
		return entityManager.createQuery("""
						select m.room.id, count(m) from ChatMessage m, ChatRoomMember rm
						where rm.room = m.room and rm.userId = :userId and m.room.id in :roomIds
						  and m.senderId <> :userId and m.deletedAt is null
						  and m.id > coalesce(rm.lastReadMessageId, 0) and m.id > coalesce(rm.clearedMessageId, 0)
						group by m.room.id
						""", Object[].class)
				.setParameter("userId", userId)
				.setParameter("roomIds", roomIds)
				.getResultStream()
				.collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
	}

	private Slice<ChatRoomMember> slice(List<ChatRoomMember> rows, Pageable pageable) {
		boolean hasNext = rows.size() > pageable.getPageSize();
		return new SliceImpl<>(hasNext ? rows.subList(0, pageable.getPageSize()) : rows, pageable, hasNext);
	}
}
