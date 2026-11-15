package com.lionlink.backend.domain.chat.service;

import com.lionlink.backend.domain.chat.dto.request.ChatMessageSendRequest;
import com.lionlink.backend.domain.chat.dto.request.ChatRoomInviteRequest;
import com.lionlink.backend.domain.chat.dto.request.ChatMessageUpdateRequest;
import com.lionlink.backend.domain.chat.dto.request.ChatPreferenceUpdateRequest;
import com.lionlink.backend.domain.chat.dto.request.ChatReadRequest;
import com.lionlink.backend.domain.chat.dto.response.ChatMessageListResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatMessageResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatPreferenceResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatReadResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatRoomInviteResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatDirectRoomResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatRoomDetailResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatUnreadCountResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatUserResponse;
import com.lionlink.backend.domain.chat.entity.ChatMessage;
import com.lionlink.backend.domain.chat.entity.ChatRoom;
import com.lionlink.backend.domain.chat.entity.ChatRoomMember;
import com.lionlink.backend.domain.chat.entity.ChatRoomStatus;
import com.lionlink.backend.domain.chat.entity.ChatRoomType;
import com.lionlink.backend.domain.chat.event.ChatNotificationEvent;
import com.lionlink.backend.domain.chat.event.ChatReadEvent;
import com.lionlink.backend.domain.chat.event.ChatRoomUpdatedEvent;
import com.lionlink.backend.domain.chat.exception.ChatErrorCode;
import com.lionlink.backend.domain.chat.repository.ChatMessageRepository;
import com.lionlink.backend.domain.chat.repository.ChatQueryRepository;
import com.lionlink.backend.domain.chat.repository.ChatRoomMemberRepository;
import com.lionlink.backend.domain.chat.repository.ChatRoomRepository;
import com.lionlink.backend.domain.coffeechat.repository.CoffeeChatRepository;
import com.lionlink.backend.domain.community.meetup.entity.Meetup;
import com.lionlink.backend.domain.community.meetup.entity.MeetupMember;
import com.lionlink.backend.domain.community.meetup.entity.MeetupMemberId;
import com.lionlink.backend.domain.community.meetup.entity.MeetupStatus;
import com.lionlink.backend.domain.community.meetup.repository.MeetupMemberRepository;
import com.lionlink.backend.domain.community.meetup.repository.MeetupRepository;
import com.lionlink.backend.domain.community.post.entity.Post;
import com.lionlink.backend.domain.community.post.repository.PostRepository;
import com.lionlink.backend.domain.community.recruit.entity.CoffeeChatRecruit;
import com.lionlink.backend.domain.community.recruit.repository.CoffeeChatRecruitRepository;
import com.lionlink.backend.domain.profile.UserSummary;
import com.lionlink.backend.domain.profile.UserSummaryReader;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.domain.user.event.UserWithdrawnEvent;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.response.SliceResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatRoomService {

	public static final String CHAT_INVITED = "CHAT_INVITED";

	private static final int COFFEE_CHAT_APPLICANT_LIMIT = 5;

	private static final int RECEIVER = 1;

	private final ChatRoomRepository chatRoomRepository;
	private final ChatRoomMemberRepository chatRoomMemberRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final ChatQueryRepository chatQueryRepository;
	private final ChatMessageWriter chatMessageWriter;
	private final CoffeeChatRepository coffeeChatRepository;
	private final MeetupRepository meetupRepository;
	private final MeetupMemberRepository meetupMemberRepository;
	private final PostRepository postRepository;
	private final CoffeeChatRecruitRepository coffeeChatRecruitRepository;
	private final UserRepository userRepository;
	private final UserSummaryReader userSummaryReader;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional(readOnly = true)
	public SliceResponse<ChatRoomSummaryResponse> getList(Long userId, Pageable pageable) {
		Slice<ChatRoomMember> mine = chatQueryRepository.findMyRooms(userId, pageable);
		List<Long> roomIds = mine.getContent().stream().map(member -> member.getRoom().getId()).toList();
		Map<Long, List<ChatRoomMember>> membersByRoom = roomIds.isEmpty() ? Map.of()
				: chatRoomMemberRepository.findByRoomIdIn(roomIds).stream()
						.collect(Collectors.groupingBy(member -> member.getRoom().getId()));
		Set<Long> userIds = new HashSet<>();
		membersByRoom.values().forEach(members -> members.forEach(member -> userIds.add(member.getUserId())));
		Map<Long, UserSummary> summaries = userSummaryReader.getSummaries(userIds);
		List<Long> meetupIds = mine.getContent().stream().map(member -> member.getRoom().getMeetupId()).filter(Objects::nonNull).toList();
		Map<Long, String> titles = meetupIds.isEmpty() ? Map.of()
				: postRepository.findAllById(meetupIds).stream().collect(Collectors.toMap(Post::getId, Post::getTitle));
		Map<Long, ChatMessage> lastMessages = chatQueryRepository.findLastMessages(roomIds);
		Map<Long, Long> unreadCounts = chatQueryRepository.countUnreadByRoom(userId, roomIds);

		return SliceResponse.from(mine, member -> {
			ChatRoom room = member.getRoom();
			List<ChatRoomMember> members = membersByRoom.getOrDefault(room.getId(), List.of());
			List<ChatUserResponse> others = others(userId, members, summaries);
			ChatMessageResponse lastMessage = Optional.ofNullable(lastMessages.get(room.getId()))
					.filter(message -> message.getId() > member.visibleFrom())
					.map(message -> ChatMessageResponse.of(message, chatMessageWriter.unreadCount(message, members)))
					.orElse(null);
			return new ChatRoomSummaryResponse(
					room.getId(),
					room.getType(),
					room.getStatus(),
					room.isRequestedBy(userId),
					room.isDirect() ? partner(userId, room, summaries) : null,
					room.isDirect() ? List.of() : others,
					(int) members.stream().filter(ChatRoomMember::isActive).count(),
					room.getCoffeeChatId(),
					room.getMeetupId(),
					room.isMeetupRoom() ? titles.get(room.getMeetupId()) : null,
					lastMessage,
					unreadCounts.getOrDefault(room.getId(), 0L),
					member.isPinned(),
					member.isMuted()
			);
		});
	}

	@Transactional(readOnly = true)
	public ChatUnreadCountResponse getUnreadCount(Long userId) {
		return new ChatUnreadCountResponse(chatQueryRepository.countUnreadMessages(userId), chatQueryRepository.countReceivedRequests(userId));
	}

	@Transactional(readOnly = true)
	public ChatDirectRoomResponse getDirectRoom(Long userId, Long otherUserId) {
		if (otherUserId.equals(userId)) {
			throw new BusinessException(ChatErrorCode.CANNOT_CHAT_SELF);
		}
		ChatRoom room = chatRoomRepository.findDirectRoom(ChatRoomType.DIRECT, userId, otherUserId)
				.filter(found -> found.getStatus() == ChatRoomStatus.ACTIVE || found.getStatus() == ChatRoomStatus.REQUESTED)
				.filter(found -> chatRoomMemberRepository.findByRoomIdAndUserId(found.getId(), userId)
						.filter(ChatRoomMember::isActive).isPresent())
				.orElseThrow(() -> new BusinessException(ChatErrorCode.ROOM_NOT_FOUND));
		return new ChatDirectRoomResponse(room.getId(), room.getStatus(), room.isRequestedBy(userId));
	}

	@Transactional(readOnly = true)
	public ChatRoomDetailResponse getDetail(Long userId, Long roomId) {
		ChatRoomMember member = findActiveMember(userId, roomId);
		return toDetail(userId, member.getRoom(), member);
	}

	@Transactional(readOnly = true)
	public ChatMessageListResponse getMessages(Long userId, Long roomId, Long before, Long after, int size) {
		ChatRoomMember member = findActiveMember(userId, roomId);
		List<ChatRoomMember> members = chatRoomMemberRepository.findByRoomId(roomId);
		PageRequest limit = PageRequest.of(0, size + 1);
		List<ChatMessage> messages;
		if (after != null) {
			messages = chatMessageRepository.findByRoomIdAndIdGreaterThanOrderByIdAsc(roomId, Math.max(after, member.visibleFrom()), limit);
		} else {
			messages = new ArrayList<>(before == null
					? chatMessageRepository.findByRoomIdAndIdGreaterThanOrderByIdDesc(roomId, member.visibleFrom(), limit)
					: chatMessageRepository.findByRoomIdAndIdGreaterThanAndIdLessThanOrderByIdDesc(roomId, member.visibleFrom(), before, limit));
		}
		boolean hasMore = messages.size() > size;
		List<ChatMessage> page = new ArrayList<>(hasMore ? messages.subList(0, size) : messages);
		if (after == null) {
			Collections.reverse(page);
		}
		return new ChatMessageListResponse(
				page.stream().map(message -> ChatMessageResponse.of(message, chatMessageWriter.unreadCount(message, members))).toList(),
				hasMore);
	}

	public ChatMessageResponse send(Long userId, Long roomId, ChatMessageSendRequest request) {
		LocalDateTime now = LocalDateTime.now();
		String clientMsgId = StringUtils.hasText(request.clientMsgId()) ? request.clientMsgId().trim() : null;
		if (clientMsgId != null) {
			ChatMessage sent = chatMessageRepository.findBySenderIdAndClientMsgId(userId, clientMsgId).orElse(null);
			if (sent != null && sent.getRoom().getId().equals(roomId)) {
				return ChatMessageResponse.of(sent, chatMessageWriter.unreadCount(sent, chatRoomMemberRepository.findByRoomId(roomId)));
			}
		}
		ChatRoomMember member = findActiveMember(userId, roomId);
		ChatRoom room = member.getRoom();
		if (!room.isOpen()) {
			throw new BusinessException(ChatErrorCode.INVALID_ROOM_STATUS);
		}
		if (room.isDirect() && !userRepository.existsByIdAndDeletedAtIsNull(partnerId(userId, room))) {
			throw new BusinessException(ChatErrorCode.INVALID_ROOM_STATUS, "탈퇴한 동문에게는 메시지를 보낼 수 없어요.");
		}
		if (room.isDirect()) {
			chatRoomMemberRepository.findByRoomId(roomId).stream()
					.filter(other -> !other.getUserId().equals(userId) && !other.isActive())
					.forEach(ChatRoomMember::rejoin);
		}
		return chatMessageWriter.writeText(room, userId, request.content().trim(), clientMsgId, now);
	}

	public ChatMessageResponse updateMessage(Long userId, Long roomId, Long messageId, ChatMessageUpdateRequest request) {
		ChatRoomMember member = findActiveMember(userId, roomId);
		ChatMessage message = findOwnMessage(userId, member, messageId);
		message.edit(request.content().trim(), LocalDateTime.now());
		return chatMessageWriter.publishUpdated(member.getRoom(), message);
	}

	public ChatMessageResponse deleteMessage(Long userId, Long roomId, Long messageId) {
		ChatRoomMember member = findActiveMember(userId, roomId);
		ChatMessage message = findOwnMessage(userId, member, messageId);
		message.delete(LocalDateTime.now());
		return chatMessageWriter.publishUpdated(member.getRoom(), message);
	}

	public ChatRoomInviteResponse invite(Long userId, Long roomId, ChatRoomInviteRequest request) {
		LocalDateTime now = LocalDateTime.now();
		ChatRoom room = findActiveMember(userId, roomId).getRoom();
		if (room.isDirect()) {
			throw new BusinessException(ChatErrorCode.FORBIDDEN_ACTION, "1:1 채팅방에는 초대할 수 없어요.");
		}
		if (!room.isOpen()) {
			throw new BusinessException(ChatErrorCode.INVALID_ROOM_STATUS);
		}
		requireInviter(userId, room);

		Set<Long> joined = chatRoomMemberRepository.findByRoomId(roomId).stream()
				.filter(ChatRoomMember::isActive)
				.map(ChatRoomMember::getUserId)
				.collect(Collectors.toSet());
		List<Long> requested = request.userIds().stream().distinct().toList();
		Set<Long> invitable = userRepository.findIdsByStatus(requested, UserStatus.APPROVED);
		List<Long> inviteeIds = requested.stream()
				.filter(id -> invitable.contains(id) && !joined.contains(id))
				.toList();
		requireCapacity(room, joined.size(), inviteeIds.size());

		for (Long inviteeId : inviteeIds) {
			addMember(room, inviteeId);
			if (room.isMeetupRoom()) {
				joinMeetup(room.getMeetupId(), inviteeId);
			}
			eventPublisher.publishEvent(new ChatNotificationEvent(inviteeId, CHAT_INVITED, roomId, userId,
					"님이 채팅방에 초대했어요."));
		}
		if (!inviteeIds.isEmpty()) {
			chatMessageWriter.writeSystem(room, names(inviteeIds) + "님이 들어왔어요.", now);
		}
		return new ChatRoomInviteResponse(inviteeIds.size(),
				requested.stream().filter(id -> !inviteeIds.contains(id)).toList());
	}

	public ChatReadResponse read(Long userId, Long roomId, ChatReadRequest request) {
		ChatRoomMember member = findActiveMember(userId, roomId);
		Long lastId = chatMessageRepository.findLastMessageId(roomId);
		Long target = request == null || request.lastMessageId() == null ? lastId
				: lastId == null ? null : Math.min(request.lastMessageId(), lastId);
		Long before = member.getLastReadMessageId();
		member.read(target);
		if (!Objects.equals(before, member.getLastReadMessageId())) {
			eventPublisher.publishEvent(new ChatReadEvent(roomId, userId, member.getLastReadMessageId()));
		}
		return new ChatReadResponse(roomId, member.getLastReadMessageId());
	}

	public ChatPreferenceResponse updatePreferences(Long userId, Long roomId, ChatPreferenceUpdateRequest request) {
		ChatRoomMember member = findActiveMember(userId, roomId);
		member.changePreferences(request.pinned(), request.muted(), LocalDateTime.now());
		return new ChatPreferenceResponse(roomId, member.isPinned(), member.isMuted());
	}

	public void leave(Long userId, Long roomId) {
		LocalDateTime now = LocalDateTime.now();
		ChatRoomMember member = findActiveMember(userId, roomId);
		ChatRoom room = member.getRoom();
		if (room.getStatus() == ChatRoomStatus.REQUESTED) {
			throw new BusinessException(ChatErrorCode.INVALID_ROOM_STATUS, "대기 중인 요청은 취소하거나 거절해 주세요.");
		}
		if (room.isDirect() || !room.isOpen()) {
			member.hide(chatMessageRepository.findLastMessageId(roomId), now);
			return;
		}
		if (room.isMeetupRoom() && meetupRepository.findById(room.getMeetupId())
				.filter(meetup -> !meetup.isCanceled() && !meetup.isEnded(now))
				.isPresent()) {
			throw new BusinessException(ChatErrorCode.INVALID_ROOM_STATUS, "번개가 끝나기 전에는 나갈 수 없어요. 번개 참여를 취소하면 방에서도 나가요.");
		}
		member.leave(now);
		String name = userSummaryReader.getSummary(userId).filter(summary -> !summary.deleted()).map(UserSummary::name).orElse("동문");
		chatMessageWriter.writeSystem(room, name + "님이 나갔어요.", now);
	}

	@EventListener
	public void onUserWithdrawn(UserWithdrawnEvent event) {
		Long userId = event.userId();
		LocalDateTime now = LocalDateTime.now();
		chatRoomRepository.findDirectByStatus(userId, ChatRoomStatus.REQUESTED).forEach(room -> {
			room.cancelRequest();
			notifyRoomUpdated(room);
		});
		for (ChatRoomMember member : chatRoomMemberRepository.findActiveByType(userId, ChatRoomType.GROUP)) {
			member.leave(now);
			if (member.getRoom().isOpen()) {
				chatMessageWriter.writeSystem(member.getRoom(), "탈퇴한 동문이 나갔어요.", now);
			}
		}
	}

	public void notifyRoomUpdated(ChatRoom room) {
		eventPublisher.publishEvent(new ChatRoomUpdatedEvent(room.getId(),
				ChatMessageWriter.recipients(chatRoomMemberRepository.findByRoomId(room.getId()))));
	}

	ChatRoomDetailResponse toDetail(Long userId, ChatRoom room, ChatRoomMember member) {
		List<ChatRoomMember> members = chatRoomMemberRepository.findByRoomId(room.getId());
		Map<Long, UserSummary> summaries = userSummaryReader.getSummaries(
				members.stream().map(ChatRoomMember::getUserId).collect(Collectors.toSet()));
		ChatRoomDetailResponse.CoffeeChat coffeeChat = room.getCoffeeChatId() == null ? null
				: coffeeChatRepository.findById(room.getCoffeeChatId())
						.map(ChatRoomDetailResponse.CoffeeChat::from)
						.orElse(null);
		return new ChatRoomDetailResponse(
				room.getId(),
				room.getType(),
				room.getStatus(),
				room.isRequestedBy(userId),
				others(userId, members, summaries),
				coffeeChat,
				room.isMeetupRoom() ? meetup(room.getMeetupId()) : null,
				member.isPinned(),
				member.isMuted(),
				room.isOpen() && !(room.isDirect() && user(summaries, partnerId(userId, room)).isDeleted())
		);
	}

	private ChatRoomDetailResponse.Meetup meetup(Long postId) {
		LocalDateTime now = LocalDateTime.now();
		return meetupRepository.findById(postId)
				.map(meetup -> new ChatRoomDetailResponse.Meetup(
						meetup.getPostId(),
						meetup.getPost().getTitle(),
						meetup.getMeetAt(),
						meetup.getPlace(),
						MeetupStatus.of(meetup, meetupMemberRepository.countByMeetupPostId(postId), now)))
				.orElse(null);
	}

	ChatRoomMember findActiveMember(Long userId, Long roomId) {
		return chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)
				.filter(ChatRoomMember::isActive)
				.orElseThrow(() -> new BusinessException(ChatErrorCode.ROOM_NOT_FOUND));
	}

	private ChatMessage findOwnMessage(Long userId, ChatRoomMember member, Long messageId) {
		if (!member.getRoom().isOpen()) {
			throw new BusinessException(ChatErrorCode.INVALID_ROOM_STATUS);
		}
		return chatMessageRepository.findById(messageId)
				.filter(message -> message.getRoom().getId().equals(member.getRoom().getId()))
				.filter(message -> message.getId() > member.visibleFrom())
				.filter(message -> message.isSentBy(userId) && !message.isDeleted())
				.orElseThrow(() -> new BusinessException(ChatErrorCode.MESSAGE_NOT_FOUND));
	}

	private void requireInviter(Long userId, ChatRoom room) {
		if (room.isMeetupRoom()) {
			if (postRepository.findById(room.getMeetupId()).filter(post -> post.isWrittenBy(userId)).isEmpty()) {
				throw new BusinessException(ChatErrorCode.FORBIDDEN_ACTION, "번개를 만든 사람만 초대할 수 있어요.");
			}
			return;
		}
		Long coffeeChatId = room.getCoffeeChatId();
		if (coffeeChatId == null || coffeeChatRepository.findById(coffeeChatId)
				.filter(chat -> userId.equals(chat.getRequesterId())).isEmpty()) {
			throw new BusinessException(ChatErrorCode.FORBIDDEN_ACTION, "커피챗을 신청한 사람만 초대할 수 있어요.");
		}
	}

	private void requireCapacity(ChatRoom room, int joined, int invitees) {
		if (invitees == 0) {
			return;
		}
		if (!room.isMeetupRoom()) {
			int capacity = coffeeChatRoomCapacity(room);
			if (joined + invitees > capacity) {
				throw new BusinessException(ChatErrorCode.ROOM_FULL, "커피챗 채팅방은 " + capacity + "명까지예요.");
			}
			return;
		}
		Meetup meetup = meetupRepository.findById(room.getMeetupId()).orElse(null);
		if (meetup == null) {
			return;
		}
		if (meetup.isClosed()) {
			throw new BusinessException(ChatErrorCode.ROOM_FULL, "모집이 마감된 번개예요.");
		}
		Integer capacity = meetup.getCapacity();
		if (capacity != null && meetupMemberRepository.countByMeetupPostId(room.getMeetupId()) + invitees > capacity) {
			throw new BusinessException(ChatErrorCode.ROOM_FULL, "번개 정원 " + capacity + "명을 넘어요.");
		}
	}

	private int coffeeChatRoomCapacity(ChatRoom room) {
		return coffeeChatRecruitRepository.findByCoffeeChatId(room.getCoffeeChatId())
				.map(CoffeeChatRecruit::getCapacity)
				.orElse(COFFEE_CHAT_APPLICANT_LIMIT) + RECEIVER;
	}

	private void addMember(ChatRoom room, Long inviteeId) {
		chatRoomMemberRepository.findByRoomIdAndUserId(room.getId(), inviteeId)
				.ifPresentOrElse(ChatRoomMember::rejoin,
						() -> chatRoomMemberRepository.save(ChatRoomMember.builder().room(room).userId(inviteeId).build()));
	}

	private void joinMeetup(Long postId, Long inviteeId) {
		if (meetupMemberRepository.existsById(new MeetupMemberId(postId, inviteeId))) {
			return;
		}
		meetupRepository.findById(postId).ifPresent(meetup ->
				meetupMemberRepository.save(MeetupMember.builder().meetup(meetup).userId(inviteeId).build()));
	}

	private String names(List<Long> userIds) {
		Map<Long, UserSummary> summaries = userSummaryReader.getSummaries(new HashSet<>(userIds));
		return userIds.stream()
				.map(id -> Optional.ofNullable(summaries.get(id)).filter(summary -> !summary.deleted())
						.map(UserSummary::name).orElse("동문"))
				.collect(Collectors.joining(", "));
	}

	private List<ChatUserResponse> others(Long userId, List<ChatRoomMember> members, Map<Long, UserSummary> summaries) {
		return members.stream()
				.filter(ChatRoomMember::isActive)
				.filter(member -> !member.getUserId().equals(userId))
				.map(member -> user(summaries, member.getUserId()))
				.toList();
	}

	private ChatUserResponse partner(Long userId, ChatRoom room, Map<Long, UserSummary> summaries) {
		return user(summaries, partnerId(userId, room));
	}

	private Long partnerId(Long userId, ChatRoom room) {
		return userId.equals(room.getUserAId()) ? room.getUserBId() : room.getUserAId();
	}

	private ChatUserResponse user(Map<Long, UserSummary> summaries, Long userId) {
		return ChatUserResponse.from(summaries.getOrDefault(userId, UserSummary.withdrawn(userId)));
	}
}
