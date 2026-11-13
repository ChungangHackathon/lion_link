package com.lionlink.backend.domain.chat.service;

import com.lionlink.backend.domain.chat.dto.response.ChatMessageResponse;
import com.lionlink.backend.domain.chat.entity.ChatMessage;
import com.lionlink.backend.domain.chat.entity.ChatMessageType;
import com.lionlink.backend.domain.chat.entity.ChatRoom;
import com.lionlink.backend.domain.chat.entity.ChatRoomMember;
import com.lionlink.backend.domain.chat.entity.ChatRoomStatus;
import com.lionlink.backend.domain.chat.event.ChatMessageSentEvent;
import com.lionlink.backend.domain.chat.repository.ChatMessageRepository;
import com.lionlink.backend.domain.chat.repository.ChatRoomMemberRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatMessageWriter {

	private final ChatMessageRepository chatMessageRepository;
	private final ChatRoomMemberRepository chatRoomMemberRepository;
	private final ApplicationEventPublisher eventPublisher;

	public ChatMessageResponse writeText(ChatRoom room, Long senderId, String content, String clientMsgId, LocalDateTime now) {
		ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
				.room(room)
				.senderId(senderId)
				.type(ChatMessageType.TEXT)
				.content(content)
				.clientMsgId(clientMsgId)
				.build());
		return publish(room, message, now);
	}

	public ChatMessageResponse writeSystem(ChatRoom room, String content, LocalDateTime now) {
		ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
				.room(room)
				.type(ChatMessageType.SYSTEM)
				.content(content)
				.build());
		return publish(room, message, now);
	}

	public long unreadCount(ChatMessage message, List<ChatRoomMember> members) {
		if (message.getType() == ChatMessageType.SYSTEM) {
			return 0;
		}
		return members.stream()
				.filter(ChatRoomMember::isActive)
				.filter(member -> !message.isSentBy(member.getUserId()))
				.filter(member -> member.getLastReadMessageId() == null || member.getLastReadMessageId() < message.getId())
				.count();
	}

	private ChatMessageResponse publish(ChatRoom room, ChatMessage message, LocalDateTime now) {
		room.touch(now);
		List<ChatRoomMember> members = chatRoomMemberRepository.findByRoomId(room.getId());
		if (message.getSenderId() != null) {
			members.stream()
					.filter(member -> member.getUserId().equals(message.getSenderId()))
					.findFirst()
					.ifPresent(sender -> sender.read(message.getId()));
		}
		ChatMessageResponse response = ChatMessageResponse.of(message, unreadCount(message, members));
		Map<Long, Boolean> recipients = recipients(members);
		if (room.getStatus() == ChatRoomStatus.REQUESTED) {
			recipients.keySet().removeIf(userId -> !room.isRequestedBy(userId));
		}
		eventPublisher.publishEvent(new ChatMessageSentEvent(room.getId(), response, recipients));
		return response;
	}

	static Map<Long, Boolean> recipients(List<ChatRoomMember> members) {
		Map<Long, Boolean> recipients = new LinkedHashMap<>();
		members.stream().filter(ChatRoomMember::isActive).forEach(member -> recipients.put(member.getUserId(), member.isMuted()));
		return recipients;
	}
}
