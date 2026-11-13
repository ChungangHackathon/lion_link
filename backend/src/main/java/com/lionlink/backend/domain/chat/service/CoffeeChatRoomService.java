package com.lionlink.backend.domain.chat.service;

import com.lionlink.backend.domain.chat.entity.ChatMessage;
import com.lionlink.backend.domain.chat.entity.ChatMessageType;
import com.lionlink.backend.domain.chat.entity.ChatRoom;
import com.lionlink.backend.domain.chat.entity.ChatRoomMember;
import com.lionlink.backend.domain.chat.entity.ChatRoomStatus;
import com.lionlink.backend.domain.chat.entity.ChatRoomType;
import com.lionlink.backend.domain.chat.repository.ChatMessageRepository;
import com.lionlink.backend.domain.chat.repository.ChatRoomMemberRepository;
import com.lionlink.backend.domain.chat.repository.ChatRoomRepository;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CoffeeChatRoomService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatRoomMemberRepository chatRoomMemberRepository;
	private final ChatMessageRepository chatMessageRepository;

	public Long open(CoffeeChat coffeeChat, List<Long> memberIds, LocalDateTime now) {
		ChatRoom room = memberIds.isEmpty() ? directRoom(coffeeChat) : groupRoom();
		room.openForCoffeeChat(coffeeChat.getId(), now);
		List<Long> participantIds = new ArrayList<>(List.of(coffeeChat.getReceiverId(), coffeeChat.getRequesterId()));
		participantIds.addAll(memberIds);
		participantIds.forEach(userId -> join(room, userId));
		return room.getId();
	}

	public void postSystemMessage(Long coffeeChatId, String content, LocalDateTime now) {
		chatRoomRepository.findByCoffeeChatId(coffeeChatId).ifPresent(room -> {
			chatMessageRepository.save(ChatMessage.builder()
					.room(room)
					.type(ChatMessageType.SYSTEM)
					.content(content)
					.build());
			room.touch(now);
		});
	}

	public void leave(Long coffeeChatId, Long userId, LocalDateTime now) {
		chatRoomRepository.findByCoffeeChatId(coffeeChatId)
				.filter(room -> room.getType() == ChatRoomType.GROUP)
				.flatMap(room -> chatRoomMemberRepository.findByRoomIdAndUserId(room.getId(), userId))
				.ifPresent(member -> member.leave(now));
	}

	private ChatRoom directRoom(CoffeeChat coffeeChat) {
		Long requesterId = coffeeChat.getRequesterId();
		Long receiverId = coffeeChat.getReceiverId();
		return chatRoomRepository.findDirectRoom(ChatRoomType.DIRECT, requesterId, receiverId)
				.orElseGet(() -> chatRoomRepository.save(ChatRoom.builder()
						.type(ChatRoomType.DIRECT)
						.userAId(Math.min(requesterId, receiverId))
						.userBId(Math.max(requesterId, receiverId))
						.status(ChatRoomStatus.ACTIVE)
						.build()));
	}

	private ChatRoom groupRoom() {
		return chatRoomRepository.save(ChatRoom.builder()
				.type(ChatRoomType.GROUP)
				.status(ChatRoomStatus.ACTIVE)
				.build());
	}

	private void join(ChatRoom room, Long userId) {
		chatRoomMemberRepository.findByRoomIdAndUserId(room.getId(), userId)
				.ifPresentOrElse(ChatRoomMember::rejoin,
						() -> chatRoomMemberRepository.save(ChatRoomMember.builder().room(room).userId(userId).build()));
	}
}
