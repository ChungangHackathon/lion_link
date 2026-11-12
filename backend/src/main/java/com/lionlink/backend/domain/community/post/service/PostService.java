package com.lionlink.backend.domain.community.post.service;

import com.lionlink.backend.domain.alumni.exception.AlumniErrorCode;
import com.lionlink.backend.domain.community.event.MeetupCanceledEvent;
import com.lionlink.backend.domain.community.exception.CommunityErrorCode;
import com.lionlink.backend.domain.community.meetup.entity.Meetup;
import com.lionlink.backend.domain.community.meetup.entity.MeetupMember;
import com.lionlink.backend.domain.community.meetup.repository.MeetupMemberRepository;
import com.lionlink.backend.domain.community.meetup.repository.MeetupRepository;
import com.lionlink.backend.domain.community.post.dto.request.PostCreateRequest;
import com.lionlink.backend.domain.community.post.dto.request.PostUpdateRequest;
import com.lionlink.backend.domain.community.post.dto.response.PostCreateResponse;
import com.lionlink.backend.domain.community.post.dto.response.PostUpdateResponse;
import com.lionlink.backend.domain.community.post.entity.Post;
import com.lionlink.backend.domain.community.post.entity.PostCategory;
import com.lionlink.backend.domain.community.post.repository.PostRepository;
import com.lionlink.backend.domain.community.recruit.entity.CoffeeChatRecruit;
import com.lionlink.backend.domain.community.recruit.entity.RecruitMember;
import com.lionlink.backend.domain.community.recruit.repository.CoffeeChatRecruitRepository;
import com.lionlink.backend.domain.community.recruit.repository.RecruitMemberRepository;
import com.lionlink.backend.domain.profile.CoffeeAvailabilityReader;
import com.lionlink.backend.domain.user.entity.UserRole;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.error.BusinessException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

	private final PostRepository postRepository;
	private final MeetupRepository meetupRepository;
	private final MeetupMemberRepository meetupMemberRepository;
	private final CoffeeChatRecruitRepository coffeeChatRecruitRepository;
	private final RecruitMemberRepository recruitMemberRepository;
	private final UserRepository userRepository;
	private final CoffeeAvailabilityReader coffeeAvailabilityReader;
	private final ApplicationEventPublisher eventPublisher;

	public PostCreateResponse create(Long userId, PostCreateRequest request) {
		PostCategory category = request.category();
		if (request.anonymous() && !category.allowsAnonymous()) {
			throw new BusinessException(CommunityErrorCode.ANONYMOUS_NOT_ALLOWED);
		}
		if ((request.deadline() != null && !category.allowsDeadline())
				|| (category == PostCategory.MEETUP) != (request.meetup() != null)
				|| (category == PostCategory.RECRUIT) != (request.recruit() != null)) {
			throw new BusinessException(CommunityErrorCode.INVALID_CATEGORY_DETAIL);
		}

		List<Long> companionIds = List.of();
		if (category == PostCategory.MEETUP) {
			validateMeetAt(request.meetup().meetAt());
			companionIds = validateCompanions(userId, request.meetup());
		}
		if (category == PostCategory.RECRUIT) {
			validateSenior(userId, request.recruit().seniorId());
		}

		Post post = postRepository.save(Post.builder()
				.authorId(userId)
				.category(category)
				.title(request.title().trim())
				.content(request.content().trim())
				.links(Objects.requireNonNullElse(request.links(), List.of()))
				.isAnonymous(request.anonymous())
				.deadline(request.deadline())
				.build());

		if (category == PostCategory.MEETUP) {
			createMeetup(post, userId, request.meetup(), companionIds);
		}
		if (category == PostCategory.RECRUIT) {
			createRecruit(post, userId, request.recruit());
		}
		return PostCreateResponse.from(post);
	}

	public PostUpdateResponse update(Long userId, Long postId, PostUpdateRequest request) {
		Post post = findPost(postId);
		if (!post.isWrittenBy(userId)) {
			throw new BusinessException(CommunityErrorCode.NOT_OWNER);
		}

		PostCategory category = Objects.requireNonNullElse(request.category(), post.getCategory());
		if (category != post.getCategory() && !(category.isGeneral() && post.getCategory().isGeneral())) {
			throw new BusinessException(CommunityErrorCode.CATEGORY_CHANGE_NOT_ALLOWED);
		}
		if (post.isAnonymous() && !category.allowsAnonymous()) {
			throw new BusinessException(CommunityErrorCode.ANONYMOUS_NOT_ALLOWED);
		}
		if ((request.deadline() != null && !category.allowsDeadline())
				|| (request.meetup() != null && category != PostCategory.MEETUP)
				|| (request.recruit() != null && category != PostCategory.RECRUIT)) {
			throw new BusinessException(CommunityErrorCode.INVALID_CATEGORY_DETAIL);
		}

		if (category == PostCategory.MEETUP) {
			updateMeetup(post, request.meetup());
		}
		if (category == PostCategory.RECRUIT && request.recruit() != null) {
			updateRecruit(userId, post, request.recruit());
		}

		LocalDate deadline = category.allowsDeadline()
				? Objects.requireNonNullElse(request.deadline(), post.getDeadline())
				: null;
		post.update(
				category,
				request.title() != null ? request.title().trim() : post.getTitle(),
				request.content() != null ? request.content().trim() : post.getContent(),
				request.links() != null ? request.links() : post.getLinks(),
				deadline
		);
		return PostUpdateResponse.from(post);
	}

	public void delete(Long userId, Long postId) {
		Post post = findPost(postId);
		boolean admin = !post.isWrittenBy(userId) && userRepository.existsByIdAndRole(userId, UserRole.ADMIN);
		if (!post.isWrittenBy(userId) && !admin) {
			throw new BusinessException(CommunityErrorCode.NOT_OWNER);
		}

		if (post.getCategory() == PostCategory.MEETUP) {
			Meetup meetup = findMeetup(postId);
			if (!admin && !meetup.isCanceled() && meetup.isEnded(LocalDateTime.now())) {
				throw new BusinessException(CommunityErrorCode.MEETUP_CLOSED, "종료된 번개는 취소할 수 없어요.");
			}
			if (!meetup.isCanceled() && !meetup.isEnded(LocalDateTime.now())) {
				notifyCanceled(post);
			}
			meetup.cancel();
		}
		if (post.getCategory() == PostCategory.RECRUIT) {
			CoffeeChatRecruit recruit = findRecruit(postId);
			if (!admin && !recruit.isOpen()) {
				throw new BusinessException(CommunityErrorCode.RECRUIT_CLOSED, "커피챗을 신청한 뒤에는 모집을 취소할 수 없어요.");
			}
		}
		post.delete();
	}

	private void notifyCanceled(Post post) {
		List<Long> memberIds = meetupMemberRepository.findByMeetupPostIdOrderByJoinedAtAscUserIdAsc(post.getId()).stream()
				.map(MeetupMember::getUserId)
				.filter(memberId -> !post.isWrittenBy(memberId))
				.toList();
		if (!memberIds.isEmpty()) {
			eventPublisher.publishEvent(new MeetupCanceledEvent(post.getId(), post.getTitle(), memberIds));
		}
	}

	private void createMeetup(Post post, Long userId, PostCreateRequest.MeetupRequest request, List<Long> companionIds) {
		Meetup meetup = meetupRepository.save(Meetup.builder()
				.post(post)
				.meetAt(request.meetAt())
				.place(request.place().trim())
				.capacity(request.capacity())
				.build());

		List<MeetupMember> members = new ArrayList<>();
		members.add(MeetupMember.builder().meetup(meetup).userId(userId).build());
		companionIds.forEach(companionId -> members.add(MeetupMember.builder().meetup(meetup).userId(companionId).build()));
		meetupMemberRepository.saveAll(members);
	}

	private void createRecruit(Post post, Long userId, PostCreateRequest.RecruitRequest request) {
		CoffeeChatRecruit recruit = coffeeChatRecruitRepository.save(CoffeeChatRecruit.builder()
				.post(post)
				.seniorId(request.seniorId())
				.capacity(request.capacity())
				.deadline(request.deadline())
				.build());
		recruitMemberRepository.save(RecruitMember.builder().recruit(recruit).userId(userId).build());
	}

	private void updateMeetup(Post post, PostUpdateRequest.MeetupRequest request) {
		Meetup meetup = findMeetup(post.getId());
		if (meetup.isCanceled() || meetup.isEnded(LocalDateTime.now())) {
			throw new BusinessException(CommunityErrorCode.MEETUP_CLOSED, "종료되거나 취소된 모임은 수정할 수 없어요.");
		}
		if (request == null) {
			return;
		}
		if (request.meetAt() != null) {
			validateMeetAt(request.meetAt());
		}
		if (request.capacity() != null && request.capacity() < meetupMemberRepository.countByMeetupPostId(post.getId())) {
			throw new BusinessException(CommunityErrorCode.CAPACITY_BELOW_MEMBERS);
		}
		meetup.update(
				Objects.requireNonNullElse(request.meetAt(), meetup.getMeetAt()),
				request.place() != null ? request.place().trim() : meetup.getPlace(),
				Objects.requireNonNullElse(request.capacity(), meetup.getCapacity())
		);
	}

	private void updateRecruit(Long userId, Post post, PostUpdateRequest.RecruitRequest request) {
		CoffeeChatRecruit recruit = findRecruit(post.getId());
		if (!recruit.isOpen()) {
			throw new BusinessException(CommunityErrorCode.RECRUIT_CLOSED, "커피챗을 신청한 뒤에는 모집 정보를 바꿀 수 없어요.");
		}
		long memberCount = recruitMemberRepository.countByRecruitPostId(post.getId());
		Long seniorId = Objects.requireNonNullElse(request.seniorId(), recruit.getSeniorId());
		if (!seniorId.equals(recruit.getSeniorId())) {
			if (memberCount > 1) {
				throw new BusinessException(CommunityErrorCode.RECRUIT_SENIOR_LOCKED);
			}
			validateSenior(userId, seniorId);
		}
		if (request.capacity() != null && request.capacity() < memberCount) {
			throw new BusinessException(CommunityErrorCode.CAPACITY_BELOW_MEMBERS);
		}
		recruit.update(
				seniorId,
				Objects.requireNonNullElse(request.capacity(), recruit.getCapacity()),
				Objects.requireNonNullElse(request.deadline(), recruit.getDeadline())
		);
	}

	private void validateMeetAt(LocalDateTime meetAt) {
		LocalDateTime now = LocalDateTime.now();
		if (meetAt.isBefore(now.plusHours(1)) || meetAt.isAfter(now.plusDays(14))) {
			throw new BusinessException(CommunityErrorCode.INVALID_MEETUP_TIME);
		}
	}

	private List<Long> validateCompanions(Long userId, PostCreateRequest.MeetupRequest request) {
		List<Long> companionIds = Objects.requireNonNullElse(request.companionIds(), List.<Long>of());
		List<Long> distinct = companionIds.stream().filter(Objects::nonNull).distinct().toList();
		if (distinct.size() != companionIds.size() || distinct.contains(userId) || distinct.size() > request.capacity() - 1) {
			throw new BusinessException(CommunityErrorCode.INVALID_COMPANIONS);
		}
		if (!distinct.isEmpty()
				&& userRepository.countByIdInAndStatusAndDeletedAtIsNull(distinct, UserStatus.APPROVED) != distinct.size()) {
			throw new BusinessException(AlumniErrorCode.USER_NOT_FOUND);
		}
		return distinct;
	}

	private void validateSenior(Long userId, Long seniorId) {
		if (seniorId.equals(userId)) {
			throw new BusinessException(CommunityErrorCode.INVALID_RECRUIT_SENIOR);
		}
		if (!userRepository.existsByIdAndStatusAndDeletedAtIsNull(seniorId, UserStatus.APPROVED)) {
			throw new BusinessException(AlumniErrorCode.USER_NOT_FOUND);
		}
		if (!coffeeAvailabilityReader.getAvailability(seniorId).groupAvailable()) {
			throw new BusinessException(CommunityErrorCode.GROUP_COFFEE_CHAT_NOT_ALLOWED);
		}
	}

	private Post findPost(Long postId) {
		return postRepository.findByIdAndDeletedAtIsNull(postId)
				.orElseThrow(() -> new BusinessException(CommunityErrorCode.POST_NOT_FOUND));
	}

	private Meetup findMeetup(Long postId) {
		return meetupRepository.findById(postId)
				.orElseThrow(() -> new BusinessException(CommunityErrorCode.MEETUP_NOT_FOUND));
	}

	private CoffeeChatRecruit findRecruit(Long postId) {
		return coffeeChatRecruitRepository.findById(postId)
				.orElseThrow(() -> new BusinessException(CommunityErrorCode.POST_NOT_FOUND));
	}
}
