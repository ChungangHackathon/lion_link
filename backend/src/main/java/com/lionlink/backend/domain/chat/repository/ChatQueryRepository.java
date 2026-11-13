package com.lionlink.backend.domain.chat.repository;

import com.lionlink.backend.domain.chat.entity.ChatRoomMember;
import com.lionlink.backend.domain.chat.entity.ChatRoomStatus;
import jakarta.persistence.EntityManager;
import java.util.EnumSet;
import java.util.List;
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

	private Slice<ChatRoomMember> slice(List<ChatRoomMember> rows, Pageable pageable) {
		boolean hasNext = rows.size() > pageable.getPageSize();
		return new SliceImpl<>(hasNext ? rows.subList(0, pageable.getPageSize()) : rows, pageable, hasNext);
	}
}
