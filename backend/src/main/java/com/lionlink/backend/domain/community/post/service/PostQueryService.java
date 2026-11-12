package com.lionlink.backend.domain.community.post.service;

import com.lionlink.backend.domain.coffeechat.entity.ReceiveMode;
import com.lionlink.backend.domain.coffeechat.repository.CoffeeChatRepository;
import com.lionlink.backend.domain.coffeechat.repository.CoffeeSettingRepository;
import com.lionlink.backend.domain.community.comment.repository.CommentRepository;
import com.lionlink.backend.domain.community.exception.CommunityErrorCode;
import com.lionlink.backend.domain.community.meetup.entity.Meetup;
import com.lionlink.backend.domain.community.meetup.entity.MeetupMember;
import com.lionlink.backend.domain.community.meetup.entity.MeetupStatus;
import com.lionlink.backend.domain.community.meetup.repository.MeetupMemberRepository;
import com.lionlink.backend.domain.community.meetup.repository.MeetupRepository;
import com.lionlink.backend.domain.community.post.dto.request.PostSearchCondition;
import com.lionlink.backend.domain.community.post.dto.response.CategoryCountResponse;
import com.lionlink.backend.domain.community.post.dto.response.PostAuthorResponse;
import com.lionlink.backend.domain.community.post.dto.response.PostDetailResponse;
import com.lionlink.backend.domain.community.post.dto.response.PostMemberResponse;
import com.lionlink.backend.domain.community.post.dto.response.PostSummaryResponse;
import com.lionlink.backend.domain.community.post.entity.Post;
import com.lionlink.backend.domain.community.post.entity.PostCategory;
import com.lionlink.backend.domain.community.post.entity.PostFile;
import com.lionlink.backend.domain.community.post.entity.PostFileType;
import com.lionlink.backend.domain.community.post.entity.PostLikeId;
import com.lionlink.backend.domain.community.post.entity.PostView;
import com.lionlink.backend.domain.community.post.repository.PostFileRepository;
import com.lionlink.backend.domain.community.post.repository.PostLikeRepository;
import com.lionlink.backend.domain.community.post.repository.PostRepository;
import com.lionlink.backend.domain.community.post.repository.PostSearchRepository;
import com.lionlink.backend.domain.community.post.repository.PostViewRepository;
import com.lionlink.backend.domain.community.recruit.entity.CoffeeChatRecruit;
import com.lionlink.backend.domain.community.recruit.entity.RecruitMember;
import com.lionlink.backend.domain.community.recruit.repository.CoffeeChatRecruitRepository;
import com.lionlink.backend.domain.community.recruit.repository.RecruitMemberRepository;
import com.lionlink.backend.domain.profile.UserSummary;
import com.lionlink.backend.domain.profile.UserSummaryReader;
import com.lionlink.backend.domain.profile.basic.entity.Profile;
import com.lionlink.backend.domain.profile.basic.repository.ProfileRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.jpa.IdCount;
import com.lionlink.backend.global.response.PageResponse;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryService {

	private final PostRepository postRepository;
	private final PostSearchRepository postSearchRepository;
	private final PostViewRepository postViewRepository;
	private final PostLikeRepository postLikeRepository;
	private final PostFileRepository postFileRepository;
	private final CommentRepository commentRepository;
	private final MeetupRepository meetupRepository;
	private final MeetupMemberRepository meetupMemberRepository;
	private final CoffeeChatRecruitRepository coffeeChatRecruitRepository;
	private final RecruitMemberRepository recruitMemberRepository;
	private final CoffeeSettingRepository coffeeSettingRepository;
	private final CoffeeChatRepository coffeeChatRepository;
	private final ProfileRepository profileRepository;
	private final UserSummaryReader userSummaryReader;

	public PageResponse<PostSummaryResponse> search(PostSearchCondition condition, Pageable pageable) {
		Page<Post> posts = postSearchRepository.search(condition, pageable);
		if (posts.isEmpty()) {
			return PageResponse.from(posts, post -> null);
		}
		ListContext context = listContext(posts.getContent());
		return PageResponse.from(posts, post -> toSummary(post, context));
	}

	@Transactional
	public PostDetailResponse getDetail(Long userId, Long postId) {
		Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
				.orElseThrow(() -> new BusinessException(CommunityErrorCode.POST_NOT_FOUND));
		LocalDateTime now = LocalDateTime.now();
		boolean firstView = !postViewRepository.existsByPostIdAndUserIdAndViewedAtAfter(postId, userId, now.minusHours(24));
		if (firstView) {
			postViewRepository.save(PostView.builder().post(post).userId(userId).build());
			postRepository.increaseViewCount(postId);
		}

		Meetup meetup = post.getCategory() == PostCategory.MEETUP ? meetupRepository.findById(postId).orElse(null) : null;
		List<Long> meetupMemberIds = meetup == null ? List.of() : orderMembers(post,
				meetupMemberRepository.findByMeetupPostIdOrderByJoinedAtAscUserIdAsc(postId).stream().map(MeetupMember::getUserId).toList());
		CoffeeChatRecruit recruit = post.getCategory() == PostCategory.RECRUIT ? coffeeChatRecruitRepository.findById(postId).orElse(null) : null;
		List<Long> recruitMemberIds = recruit == null ? List.of() : orderMembers(post,
				recruitMemberRepository.findByRecruitPostIdOrderByJoinedAtAscUserIdAsc(postId).stream().map(RecruitMember::getUserId).toList());

		Set<Long> userIds = new HashSet<>(meetupMemberIds);
		userIds.addAll(recruitMemberIds);
		if (!post.isAnonymous()) {
			userIds.add(post.getAuthorId());
		}
		if (recruit != null) {
			userIds.add(recruit.getSeniorId());
		}
		Map<Long, UserSummary> summaries = userSummaryReader.getSummaries(userIds);

		return new PostDetailResponse(
				post.getId(),
				post.getCategory(),
				post.getTitle(),
				post.getContent(),
				post.isAnonymous(),
				post.isWrittenBy(userId),
				post.isAnonymous() ? null : authorDetail(summary(summaries, post.getAuthorId())),
				Objects.requireNonNullElse(post.getLinks(), List.of()),
				postFileRepository.findByPostIdOrderBySortOrderAscIdAsc(postId).stream()
						.map(PostDetailResponse.FileResponse::from)
						.toList(),
				post.getDeadline(),
				post.getViewCount() + (firstView ? 1 : 0),
				commentRepository.countByPostIdAndDeletedAtIsNull(postId),
				postLikeRepository.countByPostId(postId),
				postLikeRepository.existsById(new PostLikeId(postId, userId)),
				meetup == null ? null : meetupDetail(post, meetup, meetupMemberIds, summaries, userId, now),
				recruit == null ? null : recruitDetail(post, recruit, recruitMemberIds, summaries, userId),
				post.getCreatedAt(),
				post.getUpdatedAt()
		);
	}

	public CategoryCountResponse countByCategory() {
		Map<PostCategory, Long> counts = new LinkedHashMap<>();
		for (PostCategory category : PostCategory.values()) {
			counts.put(category, 0L);
		}
		postRepository.countByCategory().forEach(row -> counts.put(row.getCategory(), row.getCount()));
		long total = counts.values().stream().mapToLong(Long::longValue).sum();
		return new CategoryCountResponse(total, counts);
	}

	private ListContext listContext(List<Post> posts) {
		List<Long> postIds = posts.stream().map(Post::getId).toList();
		List<Long> meetupIds = idsOf(posts, PostCategory.MEETUP);
		List<Long> recruitIds = idsOf(posts, PostCategory.RECRUIT);

		Map<Long, Meetup> meetups = meetupIds.isEmpty() ? Map.of()
				: meetupRepository.findAllById(meetupIds).stream().collect(Collectors.toMap(Meetup::getPostId, Function.identity()));
		Map<Long, CoffeeChatRecruit> recruits = recruitIds.isEmpty() ? Map.of()
				: coffeeChatRecruitRepository.findAllById(recruitIds).stream()
						.collect(Collectors.toMap(CoffeeChatRecruit::getPostId, Function.identity()));

		Set<Long> userIds = new HashSet<>();
		posts.stream().filter(post -> !post.isAnonymous()).forEach(post -> userIds.add(post.getAuthorId()));
		recruits.values().forEach(recruit -> userIds.add(recruit.getSeniorId()));

		return new ListContext(
				counts(postIds, commentRepository::countByPostIds),
				postFileRepository.findByPostIdInOrderBySortOrderAscIdAsc(postIds).stream()
						.collect(Collectors.groupingBy(file -> file.getPost().getId())),
				meetups,
				counts(meetupIds, meetupMemberRepository::countByPostIds),
				recruits,
				counts(recruitIds, recruitMemberRepository::countByPostIds),
				userSummaryReader.getSummaries(userIds),
				LocalDateTime.now()
		);
	}

	private PostDetailResponse.AuthorDetail authorDetail(UserSummary summary) {
		return new PostDetailResponse.AuthorDetail(
				summary.id(),
				summary.name(),
				summary.generation(),
				summary.lionParts(),
				summary.photoUrl(),
				summary.headline(),
				summary.deleted() ? null : profileRepository.findById(summary.id()).map(Profile::getStatus).orElse(null),
				summary.deleted()
		);
	}

	private PostDetailResponse.MeetupDetail meetupDetail(Post post, Meetup meetup, List<Long> memberIds,
			Map<Long, UserSummary> summaries, Long userId, LocalDateTime now) {
		return new PostDetailResponse.MeetupDetail(
				meetup.getMeetAt(),
				meetup.getPlace(),
				meetup.getCapacity(),
				memberIds.size(),
				MeetupStatus.of(meetup, memberIds.size(), now),
				memberIds.contains(userId),
				members(post, memberIds, summaries)
		);
	}

	private PostDetailResponse.RecruitDetail recruitDetail(Post post, CoffeeChatRecruit recruit, List<Long> memberIds,
			Map<Long, UserSummary> summaries, Long userId) {
		UserSummary senior = summary(summaries, recruit.getSeniorId());
		boolean acceptGroup = coffeeSettingRepository.findById(recruit.getSeniorId())
				.map(setting -> setting.getDefaultMode() == ReceiveMode.ON && setting.isAcceptGroup())
				.orElse(false);
		boolean isSenior = recruit.getSeniorId().equals(userId);
		return new PostDetailResponse.RecruitDetail(
				new PostDetailResponse.SeniorDetail(senior.id(), senior.name(), senior.generation(), senior.lionParts(),
						senior.photoUrl(), acceptGroup, senior.deleted()),
				recruit.getCapacity(),
				memberIds.size(),
				recruit.getDeadline(),
				recruit.getStatus(),
				memberIds.contains(userId),
				isSenior,
				!isSenior && coffeeChatRepository.existsOpenChat(userId, recruit.getSeniorId()),
				members(post, memberIds, summaries)
		);
	}

	private List<PostMemberResponse> members(Post post, List<Long> memberIds, Map<Long, UserSummary> summaries) {
		return memberIds.stream()
				.map(memberId -> PostMemberResponse.of(summary(summaries, memberId), post.isWrittenBy(memberId)))
				.toList();
	}

	private List<Long> orderMembers(Post post, List<Long> memberIds) {
		return memberIds.stream()
				.sorted(Comparator.comparing((Long memberId) -> !post.isWrittenBy(memberId)))
				.toList();
	}

	private static List<Long> idsOf(List<Post> posts, PostCategory category) {
		return posts.stream().filter(post -> post.getCategory() == category).map(Post::getId).toList();
	}

	private static Map<Long, Long> counts(List<Long> ids, Function<Collection<Long>, List<IdCount>> query) {
		return ids.isEmpty() ? Map.of() : IdCount.toMap(query.apply(ids));
	}

	private static UserSummary summary(Map<Long, UserSummary> summaries, Long userId) {
		return summaries.getOrDefault(userId, UserSummary.withdrawn(userId));
	}

	private record ListContext(
			Map<Long, Long> commentCounts,
			Map<Long, List<PostFile>> files,
			Map<Long, Meetup> meetups,
			Map<Long, Long> meetupMemberCounts,
			Map<Long, CoffeeChatRecruit> recruits,
			Map<Long, Long> recruitMemberCounts,
			Map<Long, UserSummary> summaries,
			LocalDateTime now
	) {
	}

	private static PostSummaryResponse toSummary(Post post, ListContext context) {
		List<PostFile> files = context.files().getOrDefault(post.getId(), List.of());
		return new PostSummaryResponse(
				post.getId(),
				post.getCategory(),
				post.getTitle(),
				post.isAnonymous(),
				post.isAnonymous() ? null : PostAuthorResponse.from(summary(context.summaries(), post.getAuthorId())),
				post.getViewCount(),
				context.commentCounts().getOrDefault(post.getId(), 0L),
				!files.isEmpty(),
				files.stream().filter(file -> file.getType() == PostFileType.IMAGE).map(PostFile::getUrl).findFirst().orElse(null),
				post.getDeadline(),
				meetupSummary(post, context),
				recruitSummary(post, context),
				post.getCreatedAt()
		);
	}

	private static PostSummaryResponse.MeetupSummary meetupSummary(Post post, ListContext context) {
		Meetup meetup = context.meetups().get(post.getId());
		if (meetup == null) {
			return null;
		}
		long participantCount = context.meetupMemberCounts().getOrDefault(post.getId(), 0L);
		return new PostSummaryResponse.MeetupSummary(
				meetup.getMeetAt(),
				meetup.getPlace(),
				meetup.getCapacity(),
				participantCount,
				MeetupStatus.of(meetup, participantCount, context.now())
		);
	}

	private static PostSummaryResponse.RecruitSummary recruitSummary(Post post, ListContext context) {
		CoffeeChatRecruit recruit = context.recruits().get(post.getId());
		if (recruit == null) {
			return null;
		}
		UserSummary senior = summary(context.summaries(), recruit.getSeniorId());
		return new PostSummaryResponse.RecruitSummary(
				new PostSummaryResponse.SeniorSummary(senior.id(), senior.name(), senior.generation(), senior.deleted()),
				recruit.getCapacity(),
				context.recruitMemberCounts().getOrDefault(post.getId(), 0L),
				recruit.getDeadline(),
				recruit.getStatus()
		);
	}
}
