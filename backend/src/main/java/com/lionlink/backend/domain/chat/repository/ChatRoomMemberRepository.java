package com.lionlink.backend.domain.chat.repository;

import com.lionlink.backend.domain.chat.entity.ChatRoomMember;
import com.lionlink.backend.domain.chat.entity.ChatRoomMemberId;
import com.lionlink.backend.domain.chat.entity.ChatRoomType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, ChatRoomMemberId> {

	Optional<ChatRoomMember> findByRoomIdAndUserId(Long roomId, Long userId);

	List<ChatRoomMember> findByRoomId(Long roomId);

	List<ChatRoomMember> findByRoomIdIn(Collection<Long> roomIds);

	void deleteByRoomId(Long roomId);

	@Query("select m from ChatRoomMember m join fetch m.room r where m.userId = :userId and m.leftAt is null and r.type = :type")
	List<ChatRoomMember> findActiveByType(Long userId, ChatRoomType type);
}
