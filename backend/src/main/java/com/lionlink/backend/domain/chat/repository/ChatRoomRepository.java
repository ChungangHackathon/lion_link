package com.lionlink.backend.domain.chat.repository;

import com.lionlink.backend.domain.chat.entity.ChatRoom;
import com.lionlink.backend.domain.chat.entity.ChatRoomType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

	@Query("""
			select r from ChatRoom r
			where r.type = :type
			  and ((r.userAId = :userId and r.userBId = :otherUserId) or (r.userAId = :otherUserId and r.userBId = :userId))
			""")
	Optional<ChatRoom> findDirectRoom(ChatRoomType type, Long userId, Long otherUserId);

	Optional<ChatRoom> findByCoffeeChatId(Long coffeeChatId);
}
