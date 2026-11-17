package com.lionlink.backend.domain.chat.repository;

import com.lionlink.backend.domain.chat.entity.ChatRoom;
import com.lionlink.backend.domain.chat.entity.ChatRoomStatus;
import com.lionlink.backend.domain.chat.entity.ChatRoomType;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

	@Query("""
			select r from ChatRoom r
			where r.type = :type
			  and ((r.userAId = :userId and r.userBId = :otherUserId) or (r.userAId = :otherUserId and r.userBId = :userId))
			""")
	Optional<ChatRoom> findDirectRoom(ChatRoomType type, Long userId, Long otherUserId);

	Optional<ChatRoom> findByCoffeeChatId(Long coffeeChatId);

	Optional<ChatRoom> findByMeetupId(Long meetupId);

	@Query("select r from ChatRoom r where r.status = :status and (r.userAId = :userId or r.userBId = :userId)")
	List<ChatRoom> findDirectByStatus(Long userId, ChatRoomStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from ChatRoom r where r.id = :id")
	Optional<ChatRoom> findByIdForUpdate(Long id);
}
