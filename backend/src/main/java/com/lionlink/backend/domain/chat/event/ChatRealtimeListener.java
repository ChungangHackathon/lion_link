package com.lionlink.backend.domain.chat.event;

import com.lionlink.backend.domain.chat.dto.response.ChatSocketMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ChatRealtimeListener {

	public static final String ROOM_TOPIC = "/topic/chat-rooms/";
	public static final String USER_QUEUE = "/queue/chat";

	private final SimpMessagingTemplate messagingTemplate;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onMessage(ChatMessageSentEvent event) {
		messagingTemplate.convertAndSend(ROOM_TOPIC + event.roomId(), ChatSocketMessage.message(event.roomId(), event.message()));
		event.recipients().forEach((userId, muted) -> messagingTemplate.convertAndSendToUser(
				String.valueOf(userId), USER_QUEUE, ChatSocketMessage.roomUpdated(event.roomId(), muted)));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onRoomUpdated(ChatRoomUpdatedEvent event) {
		event.recipients().forEach((userId, muted) -> messagingTemplate.convertAndSendToUser(
				String.valueOf(userId), USER_QUEUE, ChatSocketMessage.roomUpdated(event.roomId(), muted)));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onRead(ChatReadEvent event) {
		messagingTemplate.convertAndSend(ROOM_TOPIC + event.roomId(),
				ChatSocketMessage.read(event.roomId(), event.userId(), event.lastReadMessageId()));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onRequestReceived(ChatRequestReceivedEvent event) {
		messagingTemplate.convertAndSendToUser(String.valueOf(event.receiverId()), USER_QUEUE,
				ChatSocketMessage.requestReceived(event.roomId()));
	}
}
