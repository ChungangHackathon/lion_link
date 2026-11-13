package com.lionlink.backend.domain.chat.repository;

import com.lionlink.backend.domain.chat.entity.ChatMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	Optional<ChatMessage> findBySenderIdAndClientMsgId(Long senderId, String clientMsgId);

	@Query("select max(m.id) from ChatMessage m where m.room.id = :roomId")
	Long findLastMessageId(Long roomId);

	Optional<ChatMessage> findFirstByRoomIdAndIdGreaterThanOrderByIdDesc(Long roomId, Long visibleFrom);

	Optional<ChatMessage> findFirstByRoomIdAndIdGreaterThanOrderByIdAsc(Long roomId, Long visibleFrom);

	List<ChatMessage> findByRoomIdAndIdGreaterThanOrderByIdDesc(Long roomId, Long visibleFrom, Pageable pageable);

	List<ChatMessage> findByRoomIdAndIdGreaterThanAndIdLessThanOrderByIdDesc(Long roomId, Long visibleFrom, Long before,
			Pageable pageable);

	List<ChatMessage> findByRoomIdAndIdGreaterThanOrderByIdAsc(Long roomId, Long after, Pageable pageable);

	long countByRoomIdAndIdGreaterThanAndSenderIdNotAndDeletedAtIsNull(Long roomId, Long readUpTo, Long userId);

	void deleteByRoomId(Long roomId);
}
