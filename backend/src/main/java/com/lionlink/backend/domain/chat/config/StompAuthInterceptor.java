package com.lionlink.backend.domain.chat.config;

import com.lionlink.backend.domain.auth.service.JwtProvider;
import com.lionlink.backend.domain.chat.entity.ChatRoomMember;
import com.lionlink.backend.domain.chat.event.ChatRealtimeListener;
import com.lionlink.backend.domain.chat.repository.ChatRoomMemberRepository;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.security.DevHeaderCurrentUserProvider;
import java.security.Principal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

	private static final String BEARER = "Bearer ";
	private static final String USER_QUEUE = "/user" + ChatRealtimeListener.USER_QUEUE;

	private final JwtProvider jwtProvider;
	private final ObjectProvider<DevHeaderCurrentUserProvider> devProvider;
	private final UserRepository userRepository;
	private final ChatRoomMemberRepository chatRoomMemberRepository;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null) {
			return message;
		}
		if (StompCommand.CONNECT.equals(accessor.getCommand())) {
			Long userId = authenticate(accessor)
					.filter(id -> userRepository.existsByIdAndStatusAndDeletedAtIsNull(id, UserStatus.APPROVED))
					.orElseThrow(() -> new MessagingException("로그인이 필요해요."));
			accessor.setUser(new StompPrincipal(userId));
		}
		if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
			checkSubscription(userIdOf(accessor.getUser()), accessor.getDestination());
		}
		return message;
	}

	private void checkSubscription(Long userId, String destination) {
		if (destination == null) {
			throw new MessagingException("구독할 수 없는 주소예요.");
		}
		if (destination.equals(USER_QUEUE)) {
			return;
		}
		if (destination.startsWith(ChatRealtimeListener.ROOM_TOPIC)) {
			Long roomId = parseId(destination.substring(ChatRealtimeListener.ROOM_TOPIC.length()));
			boolean member = roomId != null && chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)
					.filter(ChatRoomMember::isActive)
					.isPresent();
			if (member) {
				return;
			}
		}
		throw new MessagingException("참여 중인 채팅방만 구독할 수 있어요.");
	}

	private Optional<Long> authenticate(StompHeaderAccessor accessor) {
		String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
		if (authorization != null && authorization.startsWith(BEARER)) {
			try {
				return Optional.of(jwtProvider.parseAccessToken(authorization.substring(BEARER.length()).trim()));
			} catch (BusinessException e) {
				return Optional.empty();
			}
		}
		if (authorization == null && devProvider.getIfAvailable() != null) {
			return Optional.ofNullable(parseId(accessor.getFirstNativeHeader(DevHeaderCurrentUserProvider.HEADER)));
		}
		return Optional.empty();
	}

	private Long userIdOf(Principal principal) {
		if (principal instanceof StompPrincipal stompPrincipal) {
			return stompPrincipal.userId();
		}
		throw new MessagingException("로그인이 필요해요.");
	}

	private Long parseId(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Long.valueOf(value.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
