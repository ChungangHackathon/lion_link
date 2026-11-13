package com.lionlink.backend.domain.community.meetup.service;

import com.lionlink.backend.domain.chat.service.MeetupChatRoomService;
import com.lionlink.backend.domain.community.event.MeetupCanceledEvent;
import com.lionlink.backend.domain.community.event.MeetupInvitedEvent;
import com.lionlink.backend.domain.community.exception.CommunityErrorCode;
import com.lionlink.backend.domain.community.meetup.dto.request.MeetupInviteRequest;
import com.lionlink.backend.domain.community.meetup.dto.response.MeetupInviteResponse;
import com.lionlink.backend.domain.community.meetup.dto.response.MeetupParticipationResponse;
import com.lionlink.backend.domain.community.meetup.entity.Meetup;
import com.lionlink.backend.domain.community.meetup.entity.MeetupInvite;
import com.lionlink.backend.domain.community.meetup.entity.MeetupMember;
import com.lionlink.backend.domain.community.meetup.entity.MeetupMemberId;
import com.lionlink.backend.domain.community.meetup.entity.MeetupStatus;
import com.lionlink.backend.domain.community.meetup.repository.MeetupInviteRepository;
import com.lionlink.backend.domain.community.meetup.repository.MeetupMemberRepository;
import com.lionlink.backend.domain.community.meetup.repository.MeetupRepository;
import com.lionlink.backend.domain.community.post.dto.response.PostMemberResponse;
import com.lionlink.backend.domain.community.post.entity.Post;
import com.lionlink.backend.domain.community.post.entity.PostCategory;
import com.lionlink.backend.domain.community.post.repository.PostRepository;
import com.lionlink.backend.domain.profile.UserSummary;
import com.lionlink.backend.domain.profile.UserSummaryReader;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.domain.user.event.UserWithdrawnEvent;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.error.BusinessException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MeetupService {

	private final PostRepository postRepository;
	private final MeetupRepository meetupRepository;
	private final MeetupMemberRepository meetupMemberRepository;
	private final MeetupInviteRepository meetupInviteRepository;
	private final UserRepository userRepository;
	private final UserSummaryReader userSummaryReader;
	private final MeetupChatRoomService meetupChatRoomService;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public MeetupParticipationResponse join(Long userId, Long postId) {
		Post post = findMeetupPost(postId);
		Meetup meetup = meetupRepository.findByIdForUpdate(postId)
				.orElseThrow(() -> new BusinessException(CommunityErrorCode.MEETUP_NOT_FOUND));
		if (meetupMemberRepository.existsById(new MeetupMemberId(postId, userId))) {
			return participation(post, meetup, userId);
		}
		if (meetup.isCanceled() || meetup.isEnded(LocalDateTime.now())) {
			throw new BusinessException(CommunityErrorCode.MEETUP_CLOSED);
		}
		if (meetupMemberRepository.countByMeetupPostId(postId) >= meetup.getCapacity()) {
			throw new BusinessException(CommunityErrorCode.MEETUP_FULL);
		}
		meetupMemberRepository.save(MeetupMember.builder().meetup(meetup).userId(userId).build());
		meetupChatRoomService.join(postId, userId, LocalDateTime.now());
		return participation(post, meetup, userId);
	}

	public MeetupParticipationResponse leave(Long userId, Long postId) {
		Post post = findMeetupPost(postId);
		Meetup meetup = findMeetup(postId);
		if (post.isWrittenBy(userId)) {
			throw new BusinessException(CommunityErrorCode.HOST_CANNOT_LEAVE);
		}
		MeetupMemberId memberId = new MeetupMemberId(postId, userId);
		if (!meetupMemberRepository.existsById(memberId)) {
			throw new BusinessException(CommunityErrorCode.NOT_MEETUP_MEMBER);
		}
		if (meetup.isCanceled() || meetup.isEnded(LocalDateTime.now())) {
			throw new BusinessException(CommunityErrorCode.MEETUP_CLOSED, "종료되거나 취소된 모임은 참여를 취소할 수 없어요.");
		}
		meetupMemberRepository.deleteById(memberId);
		meetupChatRoomService.leave(postId, userId, LocalDateTime.now());
		return participation(post, meetup, userId);
	}

	public MeetupInviteResponse invite(Long userId, Long postId, MeetupInviteRequest request) {
		Post post = findMeetupPost(postId);
		Meetup meetup = findMeetup(postId);
		if (!post.isWrittenBy(userId)) {
			throw new BusinessException(CommunityErrorCode.NOT_OWNER, "번개모임 작성자만 초대할 수 있어요.");
		}
		if (meetup.isCanceled() || meetup.isEnded(LocalDateTime.now())) {
			throw new BusinessException(CommunityErrorCode.MEETUP_CLOSED, "종료되거나 취소된 모임에는 초대할 수 없어요.");
		}

		List<Long> requested = request.userIds().stream().distinct().toList();
		Set<Long> activeIds = userRepository.findIdsByStatus(requested, UserStatus.APPROVED);
		Set<Long> memberIds = meetupMemberRepository.findMemberIds(postId, requested);
		Set<Long> invitedIds = meetupInviteRepository.findInviteeIds(postId, requested);
		List<Long> inviteeIds = requested.stream()
				.filter(id -> !id.equals(userId) && activeIds.contains(id) && !memberIds.contains(id) && !invitedIds.contains(id))
				.toList();

		meetupInviteRepository.saveAll(inviteeIds.stream()
				.map(inviteeId -> MeetupInvite.builder().meetup(meetup).inviterId(userId).inviteeId(inviteeId).build())
				.toList());
		if (!inviteeIds.isEmpty()) {
			eventPublisher.publishEvent(new MeetupInvitedEvent(postId, post.getTitle(), userId, inviteeIds));
		}
		List<Long> skipped = requested.stream().filter(id -> !inviteeIds.contains(id)).toList();
		return new MeetupInviteResponse(inviteeIds.size(), skipped);
	}

	@EventListener
	public void onUserWithdrawn(UserWithdrawnEvent event) {
		Long userId = event.userId();
		LocalDateTime now = LocalDateTime.now();
		for (Meetup meetup : meetupRepository.findHostedInProgress(userId, now)) {
			Post post = meetup.getPost();
			List<Long> memberIds = meetupMemberRepository.findByMeetupPostIdOrderByJoinedAtAscUserIdAsc(post.getId()).stream()
					.map(MeetupMember::getUserId)
					.filter(memberId -> !memberId.equals(userId))
					.toList();
			meetup.cancel();
			if (!memberIds.isEmpty()) {
				eventPublisher.publishEvent(new MeetupCanceledEvent(post.getId(), post.getTitle(), memberIds));
			}
			meetupChatRoomService.close(post.getId(), MeetupChatRoomService.CANCELED, now);
		}
		meetupMemberRepository.deleteAll(meetupMemberRepository.findJoinedInProgress(userId, now));
	}

	private MeetupParticipationResponse participation(Post post, Meetup meetup, Long userId) {
		List<Long> memberIds = meetupMemberRepository.findByMeetupPostIdOrderByJoinedAtAscUserIdAsc(post.getId()).stream()
				.map(MeetupMember::getUserId)
				.sorted(Comparator.comparing((Long memberId) -> !post.isWrittenBy(memberId)))
				.toList();
		Map<Long, UserSummary> summaries = userSummaryReader.getSummaries(memberIds);
		List<PostMemberResponse> members = memberIds.stream()
				.map(memberId -> PostMemberResponse.of(
						summaries.getOrDefault(memberId, UserSummary.withdrawn(memberId)), post.isWrittenBy(memberId)))
				.toList();
		return new MeetupParticipationResponse(
				post.getId(),
				memberIds.contains(userId),
				memberIds.size(),
				meetup.getCapacity(),
				MeetupStatus.of(meetup, memberIds.size(), LocalDateTime.now()),
				members
		);
	}

	private Post findMeetupPost(Long postId) {
		Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
				.orElseThrow(() -> new BusinessException(CommunityErrorCode.POST_NOT_FOUND));
		if (post.getCategory() != PostCategory.MEETUP) {
			throw new BusinessException(CommunityErrorCode.MEETUP_NOT_FOUND);
		}
		return post;
	}

	private Meetup findMeetup(Long postId) {
		return meetupRepository.findById(postId)
				.orElseThrow(() -> new BusinessException(CommunityErrorCode.MEETUP_NOT_FOUND));
	}
}
