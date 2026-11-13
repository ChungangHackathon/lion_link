package com.lionlink.backend.domain.chat.service;

import com.lionlink.backend.domain.chat.dto.request.ChatMessageSendRequest;
import com.lionlink.backend.domain.chat.dto.request.ChatPreferenceUpdateRequest;
import com.lionlink.backend.domain.chat.dto.request.ChatReadRequest;
import com.lionlink.backend.domain.chat.dto.response.ChatMessageListResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatMessageResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatPreferenceResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatReadResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatRoomDetailResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatUserResponse;
import com.lionlink.backend.domain.chat.entity.ChatMessage;
import com.lionlink.backend.domain.chat.entity.ChatRoom;
import com.lionlink.backend.domain.chat.entity.ChatRoomMember;
import com.lionlink.backend.domain.chat.entity.ChatRoomStatus;
import com.lionlink.backend.domain.chat.event.ChatReadEvent;
import com.lionlink.backend.domain.chat.event.ChatRoomUpdatedEvent;
import com.lionlink.backend.domain.chat.exception.ChatErrorCode;
import com.lionlink.backend.domain.chat.repository.ChatMessageRepository;
import com.lionlink.backend.domain.chat.repository.ChatQueryRepository;
import com.lionlink.backend.domain.chat.repository.ChatRoomMemberRepository;
import com.lionlink.backend.domain.chat.repository.ChatRoomRepository;
import com.lionlink.backend.domain.coffeechat.repository.CoffeeChatRepository;
import com.lionlink.backend.domain.community.meetup.entity.MeetupStatus;
import com.lionlink.backend.domain.community.meetup.repository.MeetupMemberRepository;
import com.lionlink.backend.domain.community.meetup.repository.MeetupRepository;
import com.lionlink.backend.domain.community.post.entity.Post;
import com.lionlink.backend.domain.community.post.repository.PostRepository;
import com.lionlink.backend.domain.profile.UserSummary;
import com.lionlink.backend.domain.profile.UserSummaryReader;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.response.SliceResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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

	private final ChatRoomRepository chatRoomRepository;
	private final ChatRoomMemberRepository chatRoomMemberRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final ChatQueryRepository chatQueryRepository;
	private final ChatMessageWriter chatMessageWriter;
	private final CoffeeChatRepository coffeeChatRepository;
	private final MeetupRepository meetupRepository;
	private final MeetupMemberRepository meetupMemberRepository;
	private final PostRepository postRepository;
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

		return SliceResponse.from(mine, member -> {
			ChatRoom room = member.getRoom();
			List<ChatRoomMember> members = membersByRoom.getOrDefault(room.getId(), List.of());
			List<ChatUserResponse> others = others(userId, members, summaries);
			ChatMessageResponse lastMessage = chatMessageRepository
					.findFirstByRoomIdAndIdGreaterThanOrderByIdDesc(room.getId(), member.visibleFrom())
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
					unreadCount(room.getId(), member, userId),
					member.isPinned(),
					member.isMuted()
			);
		});
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
		if (room.isDirect()) {
			chatRoomMemberRepository.findByRoomId(roomId).stream()
					.filter(other -> !other.getUserId().equals(userId) && !other.isActive())
					.forEach(ChatRoomMember::rejoin);
		}
		return chatMessageWriter.writeText(room, userId, request.content().trim(), clientMsgId, now);
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
						.map(chat -> new ChatRoomDetailResponse.CoffeeChat(chat.getId(), chat.getStatus(), chat.getScheduledAt()))
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
				room.isOpen()
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

	private long unreadCount(Long roomId, ChatRoomMember member, Long userId) {
		long readUpTo = Math.max(member.visibleFrom(), member.getLastReadMessageId() == null ? 0 : member.getLastReadMessageId());
		return chatMessageRepository.countByRoomIdAndIdGreaterThanAndSenderIdNotAndDeletedAtIsNull(roomId, readUpTo, userId);
	}

	private List<ChatUserResponse> others(Long userId, List<ChatRoomMember> members, Map<Long, UserSummary> summaries) {
		return members.stream()
				.filter(ChatRoomMember::isActive)
				.filter(member -> !member.getUserId().equals(userId))
				.map(member -> user(summaries, member.getUserId()))
				.toList();
	}

	private ChatUserResponse partner(Long userId, ChatRoom room, Map<Long, UserSummary> summaries) {
		Long partnerId = userId.equals(room.getUserAId()) ? room.getUserBId() : room.getUserAId();
		return user(summaries, partnerId);
	}

	private ChatUserResponse user(Map<Long, UserSummary> summaries, Long userId) {
		return ChatUserResponse.from(summaries.getOrDefault(userId, UserSummary.withdrawn(userId)));
	}
}
