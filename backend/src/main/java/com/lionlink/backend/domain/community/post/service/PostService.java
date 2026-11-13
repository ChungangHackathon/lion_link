package com.lionlink.backend.domain.community.post.service;

import com.lionlink.backend.domain.alumni.exception.AlumniErrorCode;
import com.lionlink.backend.domain.chat.service.MeetupChatRoomService;
import com.lionlink.backend.domain.community.event.MeetupCanceledEvent;
import com.lionlink.backend.domain.community.event.RecruitCanceledEvent;
import com.lionlink.backend.domain.community.exception.CommunityErrorCode;
import com.lionlink.backend.domain.community.meetup.entity.Meetup;
import com.lionlink.backend.domain.community.meetup.entity.MeetupMember;
import com.lionlink.backend.domain.community.meetup.repository.MeetupInviteRepository;
import com.lionlink.backend.domain.community.meetup.repository.MeetupMemberRepository;
import com.lionlink.backend.domain.community.meetup.repository.MeetupRepository;
import com.lionlink.backend.domain.community.post.dto.request.PostCreateRequest;
import com.lionlink.backend.domain.community.post.dto.request.PostFileRequest;
import com.lionlink.backend.domain.community.post.dto.request.PostUpdateRequest;
import com.lionlink.backend.domain.community.post.dto.response.PostCreateResponse;
import com.lionlink.backend.domain.community.post.dto.response.PostUpdateResponse;
import com.lionlink.backend.domain.community.post.entity.Post;
import com.lionlink.backend.domain.community.post.entity.PostCategory;
import com.lionlink.backend.domain.community.post.entity.PostFile;
import com.lionlink.backend.domain.community.post.entity.PostFileType;
import com.lionlink.backend.domain.community.post.repository.PostFileRepository;
import com.lionlink.backend.domain.community.post.repository.PostRepository;
import com.lionlink.backend.domain.community.recruit.entity.CoffeeChatRecruit;
import com.lionlink.backend.domain.community.recruit.entity.RecruitMember;
import com.lionlink.backend.domain.community.recruit.entity.RecruitStatus;
import com.lionlink.backend.domain.community.recruit.repository.CoffeeChatRecruitRepository;
import com.lionlink.backend.domain.community.recruit.repository.RecruitMemberRepository;
import com.lionlink.backend.domain.file.exception.FileErrorCode;
import com.lionlink.backend.domain.file.service.FileUploadService;
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
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

	private static final int MAX_IMAGES = 5;
	private static final int MAX_ATTACHMENTS = 3;

	private final PostRepository postRepository;
	private final PostFileRepository postFileRepository;
	private final MeetupRepository meetupRepository;
	private final MeetupMemberRepository meetupMemberRepository;
	private final MeetupInviteRepository meetupInviteRepository;
	private final MeetupChatRoomService meetupChatRoomService;
	private final CoffeeChatRecruitRepository coffeeChatRecruitRepository;
	private final RecruitMemberRepository recruitMemberRepository;
	private final UserRepository userRepository;
	private final CoffeeAvailabilityReader coffeeAvailabilityReader;
	private final FileUploadService fileUploadService;
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
		validateFiles(userId, request.files());

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
			PostCreateRequest.MeetupRequest meetup = request.meetup();
			createMeetup(post, userId, meetup.meetAt(), meetup.place(), meetup.capacity(), companionIds);
		}
		if (category == PostCategory.RECRUIT) {
			PostCreateRequest.RecruitRequest recruit = request.recruit();
			createRecruit(post, userId, recruit.seniorId(), recruit.capacity(), recruit.deadline());
		}
		saveFiles(post, request.files());
		return PostCreateResponse.from(post);
	}

	public PostUpdateResponse update(Long userId, Long postId, PostUpdateRequest request) {
		Post post = findPost(postId);
		if (!post.isWrittenBy(userId)) {
			throw new BusinessException(CommunityErrorCode.NOT_OWNER);
		}

		PostCategory category = Objects.requireNonNullElse(request.category(), post.getCategory());
		if (post.isAnonymous() && !category.allowsAnonymous()) {
			throw new BusinessException(CommunityErrorCode.ANONYMOUS_NOT_ALLOWED);
		}
		if ((request.deadline() != null && !category.allowsDeadline())
				|| (request.meetup() != null && category != PostCategory.MEETUP)
				|| (request.recruit() != null && category != PostCategory.RECRUIT)) {
			throw new BusinessException(CommunityErrorCode.INVALID_CATEGORY_DETAIL);
		}
		validateFiles(userId, request.files());

		if (category != post.getCategory()) {
			changeCategory(userId, post, category, request);
		} else if (category == PostCategory.MEETUP) {
			updateMeetup(post, request.meetup());
		} else if (category == PostCategory.RECRUIT && request.recruit() != null) {
			updateRecruit(userId, post, request.recruit());
		}

		if (request.files() != null) {
			postFileRepository.deleteByPostId(post.getId());
			saveFiles(post, request.files());
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
			boolean inProgress = !meetup.isCanceled() && !meetup.isEnded(LocalDateTime.now());
			if (inProgress) {
				notifyCanceled(post);
			}
			meetup.cancel();
			meetupChatRoomService.close(postId, inProgress ? MeetupChatRoomService.CANCELED : MeetupChatRoomService.DELETED,
					LocalDateTime.now());
		}
		if (post.getCategory() == PostCategory.RECRUIT) {
			CoffeeChatRecruit recruit = findRecruit(postId);
			if (!admin && !recruit.isOpen()) {
				throw new BusinessException(CommunityErrorCode.RECRUIT_CLOSED, "커피챗을 신청한 뒤에는 모집을 취소할 수 없어요.");
			}
			if (recruit.statusOn(LocalDate.now()) == RecruitStatus.OPEN) {
				notifyRecruitCanceled(post);
			}
		}
		post.delete();
	}

	private void notifyRecruitCanceled(Post post) {
		List<Long> memberIds = recruitMemberRepository.findByRecruitPostIdOrderByJoinedAtAscUserIdAsc(post.getId()).stream()
				.map(RecruitMember::getUserId)
				.filter(memberId -> !post.isWrittenBy(memberId))
				.toList();
		if (!memberIds.isEmpty()) {
			eventPublisher.publishEvent(new RecruitCanceledEvent(post.getId(), post.getTitle(), memberIds));
		}
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

	private void changeCategory(Long userId, Post post, PostCategory category, PostUpdateRequest request) {
		PostUpdateRequest.MeetupRequest newMeetup = request.meetup();
		PostUpdateRequest.RecruitRequest newRecruit = request.recruit();
		if (category == PostCategory.MEETUP) {
			if (newMeetup == null || newMeetup.meetAt() == null || newMeetup.place() == null || newMeetup.capacity() == null) {
				throw new BusinessException(CommunityErrorCode.INVALID_CATEGORY_DETAIL);
			}
			validateMeetAt(newMeetup.meetAt());
		}
		if (category == PostCategory.RECRUIT) {
			if (newRecruit == null || newRecruit.seniorId() == null || newRecruit.capacity() == null || newRecruit.deadline() == null) {
				throw new BusinessException(CommunityErrorCode.INVALID_CATEGORY_DETAIL);
			}
			validateSenior(userId, newRecruit.seniorId());
		}

		if (post.getCategory() == PostCategory.MEETUP) {
			removeMeetup(post);
		}
		if (post.getCategory() == PostCategory.RECRUIT) {
			removeRecruit(post);
		}
		if (category == PostCategory.MEETUP) {
			createMeetup(post, userId, newMeetup.meetAt(), newMeetup.place(), newMeetup.capacity(), List.of());
		}
		if (category == PostCategory.RECRUIT) {
			createRecruit(post, userId, newRecruit.seniorId(), newRecruit.capacity(), newRecruit.deadline());
		}
	}

	private void removeMeetup(Post post) {
		Meetup meetup = findMeetup(post.getId());
		if (meetup.isCanceled() || meetup.isEnded(LocalDateTime.now())) {
			throw new BusinessException(CommunityErrorCode.MEETUP_CLOSED, "종료되거나 취소된 모임은 수정할 수 없어요.");
		}
		if (meetupMemberRepository.countByMeetupPostId(post.getId()) > 1) {
			throw new BusinessException(CommunityErrorCode.CATEGORY_CHANGE_NOT_ALLOWED);
		}
		meetupChatRoomService.delete(post.getId());
		meetupInviteRepository.deleteByMeetupPostId(post.getId());
		meetupMemberRepository.deleteByMeetupPostId(post.getId());
		meetupRepository.delete(meetup);
	}

	private void removeRecruit(Post post) {
		CoffeeChatRecruit recruit = findRecruit(post.getId());
		if (!recruit.isOpen()) {
			throw new BusinessException(CommunityErrorCode.RECRUIT_CLOSED, "커피챗을 신청한 뒤에는 모집 정보를 바꿀 수 없어요.");
		}
		if (recruitMemberRepository.countByRecruitPostId(post.getId()) > 1) {
			throw new BusinessException(CommunityErrorCode.CATEGORY_CHANGE_NOT_ALLOWED);
		}
		recruitMemberRepository.deleteByRecruitPostId(post.getId());
		coffeeChatRecruitRepository.delete(recruit);
	}

	private void createMeetup(Post post, Long userId, LocalDateTime meetAt, String place, Integer capacity,
			List<Long> companionIds) {
		Meetup meetup = meetupRepository.save(Meetup.builder()
				.post(post)
				.meetAt(meetAt)
				.place(place.trim())
				.capacity(capacity)
				.build());

		List<MeetupMember> members = new ArrayList<>();
		members.add(MeetupMember.builder().meetup(meetup).userId(userId).build());
		companionIds.forEach(companionId -> members.add(MeetupMember.builder().meetup(meetup).userId(companionId).build()));
		meetupMemberRepository.saveAll(members);
		meetupChatRoomService.open(post.getId(), LocalDateTime.now());
	}

	private void createRecruit(Post post, Long userId, Long seniorId, Integer capacity, LocalDate deadline) {
		CoffeeChatRecruit recruit = coffeeChatRecruitRepository.save(CoffeeChatRecruit.builder()
				.post(post)
				.seniorId(seniorId)
				.capacity(capacity)
				.deadline(deadline)
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

	private void validateFiles(Long userId, List<PostFileRequest> files) {
		if (files == null) {
			return;
		}
		long images = files.stream().filter(file -> file.type() == PostFileType.IMAGE).count();
		if (images > MAX_IMAGES || files.size() - images > MAX_ATTACHMENTS) {
			throw new BusinessException(CommunityErrorCode.FILE_LIMIT_EXCEEDED);
		}
		if (files.stream().anyMatch(file -> !fileUploadService.isUploadedBy(userId, file.url()))) {
			throw new BusinessException(FileErrorCode.INVALID_FILE_URL);
		}
	}

	private void saveFiles(Post post, List<PostFileRequest> files) {
		if (files == null || files.isEmpty()) {
			return;
		}
		postFileRepository.saveAll(IntStream.range(0, files.size())
				.mapToObj(index -> PostFile.builder()
						.post(post)
						.type(files.get(index).type())
						.url(files.get(index).url())
						.name(files.get(index).name().trim())
						.size(files.get(index).size())
						.sortOrder(index)
						.build())
				.toList());
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
