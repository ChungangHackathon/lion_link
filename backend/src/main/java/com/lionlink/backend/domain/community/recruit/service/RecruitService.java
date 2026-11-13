package com.lionlink.backend.domain.community.recruit.service;

import com.lionlink.backend.domain.coffeechat.repository.CoffeeChatRepository;
import com.lionlink.backend.domain.community.event.RecruitJoinedEvent;
import com.lionlink.backend.domain.community.exception.CommunityErrorCode;
import com.lionlink.backend.domain.community.post.dto.response.PostMemberResponse;
import com.lionlink.backend.domain.community.post.entity.Post;
import com.lionlink.backend.domain.community.post.entity.PostCategory;
import com.lionlink.backend.domain.community.post.repository.PostRepository;
import com.lionlink.backend.domain.community.recruit.dto.response.RecruitParticipationResponse;
import com.lionlink.backend.domain.community.recruit.entity.CoffeeChatRecruit;
import com.lionlink.backend.domain.community.recruit.entity.RecruitMember;
import com.lionlink.backend.domain.community.recruit.entity.RecruitMemberId;
import com.lionlink.backend.domain.community.recruit.entity.RecruitStatus;
import com.lionlink.backend.domain.community.recruit.repository.CoffeeChatRecruitRepository;
import com.lionlink.backend.domain.community.recruit.repository.RecruitMemberRepository;
import com.lionlink.backend.domain.profile.UserSummary;
import com.lionlink.backend.domain.profile.UserSummaryReader;
import com.lionlink.backend.global.error.BusinessException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecruitService {

	private final PostRepository postRepository;
	private final CoffeeChatRecruitRepository coffeeChatRecruitRepository;
	private final RecruitMemberRepository recruitMemberRepository;
	private final CoffeeChatRepository coffeeChatRepository;
	private final UserSummaryReader userSummaryReader;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public RecruitParticipationResponse join(Long userId, Long postId) {
		Post post = findRecruitPost(postId);
		CoffeeChatRecruit recruit = coffeeChatRecruitRepository.findByIdForUpdate(postId)
				.orElseThrow(() -> new BusinessException(CommunityErrorCode.RECRUIT_NOT_FOUND));
		LocalDate today = LocalDate.now();
		if (recruitMemberRepository.existsById(new RecruitMemberId(postId, userId))) {
			return participation(post, recruit, userId, today);
		}
		if (recruit.getSeniorId().equals(userId)) {
			throw new BusinessException(CommunityErrorCode.SENIOR_CANNOT_JOIN);
		}
		if (recruit.statusOn(today) != RecruitStatus.OPEN) {
			throw new BusinessException(CommunityErrorCode.RECRUIT_CLOSED);
		}
		if (recruitMemberRepository.countByRecruitPostId(postId) >= recruit.getCapacity()) {
			throw new BusinessException(CommunityErrorCode.RECRUIT_FULL);
		}
		if (coffeeChatRepository.existsOpenChatWithAny(recruit.getSeniorId(), List.of(userId))) {
			throw new BusinessException(CommunityErrorCode.OPEN_COFFEE_CHAT_EXISTS);
		}

		recruitMemberRepository.save(RecruitMember.builder().recruit(recruit).userId(userId).build());
		eventPublisher.publishEvent(new RecruitJoinedEvent(postId, post.getTitle(), post.getAuthorId(), userId));
		return participation(post, recruit, userId, today);
	}

	private RecruitParticipationResponse participation(Post post, CoffeeChatRecruit recruit, Long userId, LocalDate today) {
		List<Long> memberIds = recruitMemberRepository.findByRecruitPostIdOrderByJoinedAtAscUserIdAsc(post.getId()).stream()
				.map(RecruitMember::getUserId)
				.sorted(Comparator.comparing((Long memberId) -> !post.isWrittenBy(memberId)))
				.toList();
		Map<Long, UserSummary> summaries = userSummaryReader.getSummaries(memberIds);
		List<PostMemberResponse> members = memberIds.stream()
				.map(memberId -> PostMemberResponse.of(
						summaries.getOrDefault(memberId, UserSummary.withdrawn(memberId)), post.isWrittenBy(memberId)))
				.toList();
		return new RecruitParticipationResponse(
				post.getId(),
				memberIds.contains(userId),
				memberIds.size(),
				recruit.getCapacity(),
				recruit.statusOn(today),
				members
		);
	}

	private Post findRecruitPost(Long postId) {
		Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
				.orElseThrow(() -> new BusinessException(CommunityErrorCode.POST_NOT_FOUND));
		if (post.getCategory() != PostCategory.RECRUIT) {
			throw new BusinessException(CommunityErrorCode.RECRUIT_NOT_FOUND);
		}
		return post;
	}
}
