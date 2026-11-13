package com.lionlink.backend.domain.chat.service;

import com.lionlink.backend.domain.alumni.exception.AlumniErrorCode;
import com.lionlink.backend.domain.chat.dto.request.ChatRequestCreateRequest;
import com.lionlink.backend.domain.chat.dto.request.ChatRequestDeclineRequest;
import com.lionlink.backend.domain.chat.dto.request.ChatRequestMessageUpdateRequest;
import com.lionlink.backend.domain.chat.dto.response.ChatMessageResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatRequestCreateResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatRequestDetailResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatRequestSummaryResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatRoomDetailResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatUserResponse;
import com.lionlink.backend.domain.chat.entity.ChatDeclineReason;
import com.lionlink.backend.domain.chat.entity.ChatMessage;
import com.lionlink.backend.domain.chat.entity.ChatRoom;
import com.lionlink.backend.domain.chat.entity.ChatRoomMember;
import com.lionlink.backend.domain.chat.entity.ChatRoomStatus;
import com.lionlink.backend.domain.chat.entity.ChatRoomType;
import com.lionlink.backend.domain.chat.event.ChatNotificationEvent;
import com.lionlink.backend.domain.chat.event.ChatRequestReceivedEvent;
import com.lionlink.backend.domain.chat.exception.ChatErrorCode;
import com.lionlink.backend.domain.chat.repository.ChatMessageRepository;
import com.lionlink.backend.domain.chat.repository.ChatQueryRepository;
import com.lionlink.backend.domain.chat.repository.ChatRoomMemberRepository;
import com.lionlink.backend.domain.chat.repository.ChatRoomRepository;
import com.lionlink.backend.domain.profile.UserSummary;
import com.lionlink.backend.domain.profile.UserSummaryReader;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.response.SliceResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatRequestService {

	public static final String MESSAGE_REQUESTED = "MESSAGE_REQUESTED";
	public static final String MESSAGE_REQUEST_ACCEPTED = "MESSAGE_REQUEST_ACCEPTED";
	private static final int REQUEST_WAIT_DAYS = 7;
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("M월 d일");

	private final ChatRoomRepository chatRoomRepository;
	private final ChatRoomMemberRepository chatRoomMemberRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final ChatQueryRepository chatQueryRepository;
	private final ChatMessageWriter chatMessageWriter;
	private final ChatRoomService chatRoomService;
	private final UserRepository userRepository;
	private final UserSummaryReader userSummaryReader;
	private final ApplicationEventPublisher eventPublisher;

	public ChatRequestCreateResponse create(Long userId, ChatRequestCreateRequest request) {
		LocalDateTime now = LocalDateTime.now();
		Long receiverId = request.receiverId();
		if (receiverId.equals(userId)) {
			throw new BusinessException(ChatErrorCode.CANNOT_CHAT_SELF);
		}
		if (!userRepository.existsByIdAndStatusAndDeletedAtIsNull(receiverId, UserStatus.APPROVED)) {
			throw new BusinessException(AlumniErrorCode.USER_NOT_FOUND);
		}

		ChatRoom room = chatRoomRepository.findDirectRoom(ChatRoomType.DIRECT, userId, receiverId).orElse(null);
		if (room != null && (room.getStatus() == ChatRoomStatus.ACTIVE || room.getStatus() == ChatRoomStatus.REQUESTED)) {
			join(room, userId).rejoin();
			return new ChatRequestCreateResponse(room.getId(), room.getStatus(), room.isRequestedBy(userId), false);
		}
		if (room != null && room.getStatus() == ChatRoomStatus.DECLINED && room.isRequestedBy(userId)) {
			LocalDate availableDate = room.getDeclinedAt().toLocalDate().plusDays(REQUEST_WAIT_DAYS);
			if (availableDate.isAfter(now.toLocalDate())) {
				throw new BusinessException(ChatErrorCode.REQUEST_COOLDOWN, availableDate.format(DAY) + "부터 다시 요청할 수 있어요.");
			}
		}

		if (room == null) {
			room = chatRoomRepository.save(ChatRoom.builder()
					.type(ChatRoomType.DIRECT)
					.userAId(Math.min(userId, receiverId))
					.userBId(Math.max(userId, receiverId))
					.status(ChatRoomStatus.REQUESTED)
					.requestedBy(userId)
					.requestedAt(now)
					.build());
		} else {
			Long lastId = chatMessageRepository.findLastMessageId(room.getId());
			room.request(userId, now);
			for (Long memberId : List.of(userId, receiverId)) {
				ChatRoomMember member = join(room, memberId);
				member.rejoin();
				if (lastId != null) {
					member.hide(lastId, now);
					member.rejoin();
				}
			}
		}
		join(room, userId);
		join(room, receiverId);
		chatMessageWriter.writeText(room, userId, request.content().trim(), null, now);

		eventPublisher.publishEvent(new ChatNotificationEvent(receiverId, MESSAGE_REQUESTED, room.getId(), userId, "님이 메시지를 보냈어요."));
		eventPublisher.publishEvent(new ChatRequestReceivedEvent(room.getId(), receiverId));
		return new ChatRequestCreateResponse(room.getId(), room.getStatus(), true, true);
	}

	@Transactional(readOnly = true)
	public SliceResponse<ChatRequestSummaryResponse> getReceived(Long userId, Pageable pageable) {
		Slice<ChatRoomMember> requests = chatQueryRepository.findReceivedRequests(userId, pageable);
		Map<Long, UserSummary> summaries = userSummaryReader.getSummaries(
				requests.getContent().stream().map(member -> member.getRoom().getRequestedBy()).toList());
		return SliceResponse.from(requests, member -> new ChatRequestSummaryResponse(
				member.getRoom().getId(),
				user(summaries, member.getRoom().getRequestedBy()),
				firstMessage(member),
				member.getRoom().getRequestedAt()
		));
	}

	@Transactional(readOnly = true)
	public ChatRequestDetailResponse getDetail(Long userId, Long roomId) {
		ChatRoomMember member = findRequestMember(userId, roomId);
		ChatRoom room = member.getRoom();
		Long receiverId = receiverOf(room);
		Map<Long, UserSummary> summaries = userSummaryReader.getSummaries(Set.of(room.getRequestedBy(), receiverId));
		return new ChatRequestDetailResponse(
				room.getId(),
				room.getStatus(),
				user(summaries, room.getRequestedBy()),
				user(summaries, receiverId),
				firstMessage(member),
				room.getRequestedAt(),
				room.getDeclinedAt(),
				room.getDeclineReason(),
				room.getDeclineMessage()
		);
	}

	public ChatRoomDetailResponse accept(Long userId, Long roomId) {
		LocalDateTime now = LocalDateTime.now();
		ChatRoomMember member = findRequestMember(userId, roomId);
		ChatRoom room = lockRequested(roomId);
		if (room.isRequestedBy(userId)) {
			throw new BusinessException(ChatErrorCode.FORBIDDEN_ACTION);
		}
		room.acceptRequest(now);
		member.read(chatMessageWriter.writeSystem(room, "메시지 요청을 수락했어요. 대화를 시작해보세요.", now).id());
		eventPublisher.publishEvent(new ChatNotificationEvent(room.getRequestedBy(), MESSAGE_REQUEST_ACCEPTED, roomId, userId,
				"님이 메시지 요청을 수락했어요."));
		return chatRoomService.toDetail(userId, room, member);
	}

	public void decline(Long userId, Long roomId, ChatRequestDeclineRequest request) {
		findRequestMember(userId, roomId);
		ChatRoom room = lockRequested(roomId);
		if (room.isRequestedBy(userId)) {
			throw new BusinessException(ChatErrorCode.FORBIDDEN_ACTION);
		}
		String message = request.reason() == ChatDeclineReason.ETC ? request.message().trim() : null;
		room.decline(request.reason(), message, LocalDateTime.now());
		chatRoomService.notifyRoomUpdated(room);
	}

	public ChatMessageResponse updateMessage(Long userId, Long roomId, Long messageId, ChatRequestMessageUpdateRequest request) {
		findRequestMember(userId, roomId);
		ChatRoom room = lockRequested(roomId);
		ChatMessage message = findOwnMessage(userId, room, messageId);
		message.edit(request.content().trim(), LocalDateTime.now());
		return ChatMessageResponse.of(message, chatMessageWriter.unreadCount(message, chatRoomMemberRepository.findByRoomId(roomId)));
	}

	public void cancel(Long userId, Long roomId, Long messageId) {
		findRequestMember(userId, roomId);
		ChatRoom room = lockRequested(roomId);
		ChatMessage message = findOwnMessage(userId, room, messageId);
		message.delete(LocalDateTime.now());
		room.cancelRequest();
		chatRoomService.notifyRoomUpdated(room);
	}

	private ChatRoomMember findRequestMember(Long userId, Long roomId) {
		ChatRoomMember member = chatRoomService.findActiveMember(userId, roomId);
		if (!member.getRoom().isDirect() || member.getRoom().getRequestedBy() == null) {
			throw new BusinessException(ChatErrorCode.ROOM_NOT_FOUND);
		}
		return member;
	}

	private ChatRoom lockRequested(Long roomId) {
		ChatRoom room = chatRoomRepository.findByIdForUpdate(roomId)
				.orElseThrow(() -> new BusinessException(ChatErrorCode.ROOM_NOT_FOUND));
		if (room.getStatus() != ChatRoomStatus.REQUESTED) {
			throw new BusinessException(ChatErrorCode.INVALID_ROOM_STATUS);
		}
		return room;
	}

	private ChatMessage findOwnMessage(Long userId, ChatRoom room, Long messageId) {
		if (!room.isRequestedBy(userId)) {
			throw new BusinessException(ChatErrorCode.FORBIDDEN_ACTION);
		}
		return chatMessageRepository.findById(messageId)
				.filter(message -> message.getRoom().getId().equals(room.getId()) && message.isSentBy(userId) && !message.isDeleted())
				.orElseThrow(() -> new BusinessException(ChatErrorCode.MESSAGE_NOT_FOUND));
	}

	private ChatMessageResponse firstMessage(ChatRoomMember member) {
		return chatMessageRepository.findFirstByRoomIdAndIdGreaterThanOrderByIdAsc(member.getRoom().getId(), member.visibleFrom())
				.map(message -> ChatMessageResponse.of(message, 0))
				.orElse(null);
	}

	private ChatRoomMember join(ChatRoom room, Long userId) {
		return chatRoomMemberRepository.findByRoomIdAndUserId(room.getId(), userId)
				.orElseGet(() -> chatRoomMemberRepository.save(ChatRoomMember.builder().room(room).userId(userId).build()));
	}

	private Long receiverOf(ChatRoom room) {
		return room.isRequestedBy(room.getUserAId()) ? room.getUserBId() : room.getUserAId();
	}

	private ChatUserResponse user(Map<Long, UserSummary> summaries, Long userId) {
		return ChatUserResponse.from(summaries.getOrDefault(userId, UserSummary.withdrawn(userId)));
	}
}
