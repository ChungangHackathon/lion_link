package com.lionlink.backend.domain.chat.service;

import com.lionlink.backend.domain.chat.entity.ChatRoom;
import com.lionlink.backend.domain.chat.entity.ChatRoomMember;
import com.lionlink.backend.domain.chat.entity.ChatRoomStatus;
import com.lionlink.backend.domain.chat.entity.ChatRoomType;
import com.lionlink.backend.domain.chat.repository.ChatMessageRepository;
import com.lionlink.backend.domain.chat.repository.ChatRoomMemberRepository;
import com.lionlink.backend.domain.chat.repository.ChatRoomRepository;
import com.lionlink.backend.domain.community.meetup.entity.MeetupMember;
import com.lionlink.backend.domain.community.meetup.repository.MeetupMemberRepository;
import com.lionlink.backend.domain.profile.UserSummary;
import com.lionlink.backend.domain.profile.UserSummaryReader;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MeetupChatRoomService {

	public static final String CANCELED = "번개가 취소됐어요.";
	public static final String DELETED = "번개 글이 삭제됐어요.";

	private final ChatRoomRepository chatRoomRepository;
	private final ChatRoomMemberRepository chatRoomMemberRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final MeetupMemberRepository meetupMemberRepository;
	private final ChatMessageWriter chatMessageWriter;
	private final UserSummaryReader userSummaryReader;

	public Long open(Long postId, LocalDateTime now) {
		ChatRoom room = chatRoomRepository.save(ChatRoom.builder()
				.type(ChatRoomType.GROUP)
				.status(ChatRoomStatus.ACTIVE)
				.meetupId(postId)
				.build());
		meetupMemberRepository.findByMeetupPostIdOrderByJoinedAtAscUserIdAsc(postId).stream()
				.map(MeetupMember::getUserId)
				.forEach(userId -> addMember(room, userId));
		chatMessageWriter.writeSystem(room, "번개 채팅방이 열렸어요. 모임 전에 인사 나눠보세요.", now);
		return room.getId();
	}

	public void join(Long postId, Long userId, LocalDateTime now) {
		ChatRoom room = chatRoomRepository.findByMeetupId(postId).orElse(null);
		if (room == null) {
			open(postId, now);
			return;
		}
		boolean joined = chatRoomMemberRepository.findByRoomIdAndUserId(room.getId(), userId)
				.filter(ChatRoomMember::isActive)
				.isPresent();
		if (!room.isOpen() || joined) {
			return;
		}
		addMember(room, userId);
		chatMessageWriter.writeSystem(room, name(userId) + "님이 들어왔어요.", now);
	}

	public void leave(Long postId, Long userId, LocalDateTime now) {
		chatRoomRepository.findByMeetupId(postId)
				.filter(ChatRoom::isOpen)
				.ifPresent(room -> chatRoomMemberRepository.findByRoomIdAndUserId(room.getId(), userId)
						.filter(ChatRoomMember::isActive)
						.ifPresent(member -> {
							member.leave(now);
							chatMessageWriter.writeSystem(room, name(userId) + "님이 나갔어요.", now);
						}));
	}

	public void close(Long postId, String message, LocalDateTime now) {
		chatRoomRepository.findByMeetupId(postId)
				.filter(ChatRoom::isOpen)
				.ifPresent(room -> {
					chatMessageWriter.writeSystem(room, message, now);
					room.close(now);
				});
	}

	public void delete(Long postId) {
		chatRoomRepository.findByMeetupId(postId).ifPresent(room -> {
			chatMessageRepository.deleteByRoomId(room.getId());
			chatRoomMemberRepository.deleteByRoomId(room.getId());
			chatRoomRepository.delete(room);
		});
	}

	private void addMember(ChatRoom room, Long userId) {
		chatRoomMemberRepository.findByRoomIdAndUserId(room.getId(), userId)
				.ifPresentOrElse(ChatRoomMember::rejoin,
						() -> chatRoomMemberRepository.save(ChatRoomMember.builder().room(room).userId(userId).build()));
	}

	private String name(Long userId) {
		return userSummaryReader.getSummary(userId).filter(summary -> !summary.deleted()).map(UserSummary::name).orElse("동문");
	}
}
