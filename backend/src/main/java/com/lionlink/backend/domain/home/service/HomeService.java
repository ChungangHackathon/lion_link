package com.lionlink.backend.domain.home.service;

import com.lionlink.backend.domain.alumni.service.AlumniService;
import com.lionlink.backend.domain.chat.repository.ChatQueryRepository;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChat;
import com.lionlink.backend.domain.community.post.entity.Post;
import com.lionlink.backend.domain.community.post.entity.PostFile;
import com.lionlink.backend.domain.community.post.entity.PostFileType;
import com.lionlink.backend.domain.community.post.repository.PostFileRepository;
import com.lionlink.backend.domain.community.post.repository.PostRepository;
import com.lionlink.backend.domain.home.dto.response.GuideDismissResponse;
import com.lionlink.backend.domain.home.dto.response.HomeResponse;
import com.lionlink.backend.domain.home.repository.HomeQueryRepository;
import com.lionlink.backend.domain.profile.UserSummary;
import com.lionlink.backend.domain.profile.UserSummaryReader;
import com.lionlink.backend.domain.profile.basic.entity.Profile;
import com.lionlink.backend.domain.profile.basic.repository.ProfileRepository;
import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.CommonErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

	private static final int RECOMMENDED_SENIORS = 2;

	private final UserRepository userRepository;
	private final ProfileRepository profileRepository;
	private final PostRepository postRepository;
	private final PostFileRepository postFileRepository;
	private final HomeQueryRepository homeQueryRepository;
	private final ChatQueryRepository chatQueryRepository;
	private final AlumniService alumniService;
	private final UserSummaryReader userSummaryReader;

	public HomeResponse getHome(Long userId) {
		User user = findUser(userId);
		return new HomeResponse(
				me(user, profileRepository.findById(userId).orElse(null)),
				user.getGuideDismissedAt() == null,
				new HomeResponse.Counts(
						homeQueryRepository.countPendingCoffeeChats(userId),
						homeQueryRepository.countActiveCoffeeChats(userId),
						chatQueryRepository.countUnreadMessages(userId),
						chatQueryRepository.countReceivedRequests(userId),
						alumniService.countBookmarks(userId)
				),
				homeQueryRepository.findUpcomingCoffeeChat(userId, LocalDateTime.now())
						.map(coffeeChat -> upcoming(coffeeChat, userId))
						.orElse(null),
				alumniService.recommendSeniors(userId, RECOMMENDED_SENIORS),
				onThisDay()
		);
	}

	@Transactional
	public GuideDismissResponse dismissGuide(Long userId) {
		User user = findUser(userId);
		user.dismissGuide();
		return new GuideDismissResponse(false, user.getGuideDismissedAt());
	}

	private HomeResponse.Me me(User user, Profile profile) {
		return new HomeResponse.Me(
				user.getId(),
				user.getName(),
				user.getGeneration(),
				profile == null ? List.of() : profile.getLionParts(),
				profile == null ? null : profile.getHeadline(),
				profile == null ? null : profile.getBio(),
				profile == null ? null : profile.getPhotoUrl()
		);
	}

	private HomeResponse.UpcomingCoffeeChat upcoming(CoffeeChat coffeeChat, Long userId) {
		Long partnerId = coffeeChat.getReceiverId().equals(userId) ? coffeeChat.getRequesterId() : coffeeChat.getReceiverId();
		UserSummary partner = userSummaryReader.getSummary(partnerId).orElse(UserSummary.withdrawn(partnerId));
		return new HomeResponse.UpcomingCoffeeChat(
				coffeeChat.getId(),
				coffeeChat.getScheduledAt(),
				coffeeChat.getMeetingMode(),
				homeQueryRepository.findCoffeeChatRoomId(coffeeChat.getId()).orElse(null),
				new HomeResponse.Partner(partner.id(), partner.name(), partner.generation(), partner.lionParts(),
						partner.photoUrl(), partner.deleted())
		);
	}

	private HomeResponse.OnThisDay onThisDay() {
		LocalDate day = LocalDate.now().minusYears(1);
		return postRepository.findFirstByDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByViewCountDescIdDesc(
						day.atStartOfDay(), day.plusDays(1).atStartOfDay())
				.map(this::onThisDay)
				.orElse(null);
	}

	private HomeResponse.OnThisDay onThisDay(Post post) {
		String thumbnailUrl = postFileRepository.findByPostIdOrderBySortOrderAscIdAsc(post.getId()).stream()
				.filter(file -> file.getType() == PostFileType.IMAGE)
				.map(PostFile::getUrl)
				.findFirst()
				.orElse(null);
		return new HomeResponse.OnThisDay(post.getId(), post.getCategory(), post.getTitle(), thumbnailUrl, post.getCreatedAt());
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHENTICATED));
	}
}
