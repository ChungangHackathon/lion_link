package com.lionlink.backend.domain.admin.service;

import com.lionlink.backend.domain.admin.dto.response.PendingCountResponse;
import com.lionlink.backend.domain.admin.dto.response.SignupListItemResponse;
import com.lionlink.backend.domain.profile.basic.entity.Profile;
import com.lionlink.backend.domain.profile.basic.repository.ProfileRepository;
import com.lionlink.backend.domain.profile.section.career.Career;
import com.lionlink.backend.domain.profile.section.career.CareerRepository;
import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.domain.user.repository.UserIdentity;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.response.PageResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSignupService {

	private final UserRepository userRepository;
	private final ProfileRepository profileRepository;
	private final CareerRepository careerRepository;

	/**
	 * ADMIN-01. 목록 한 페이지를 읽은 뒤, 그 페이지에 필요한 부가 정보(프로필·재직 회사·처리 관리자·같은 이름 기수)를
	 * 회원 id를 모아 한 번씩만 조회해서 붙인다. 쿼리 수는 페이지 크기와 상관없이 일정하다.
	 */
	public PageResponse<SignupListItemResponse> getSignups(UserStatus status, int page, int size) {
		Page<User> users = userRepository.findSignups(status, PageRequest.of(page, size, sortOf(status)));
		List<User> content = users.getContent();
		if (content.isEmpty()) {
			return new PageResponse<>(List.of(), users.getNumber(), users.getSize(), users.getTotalElements(), false);
		}

		List<Long> userIds = content.stream().map(User::getId).toList();
		Map<Long, Profile> profiles = profileRepository.findAllById(userIds).stream()
				.collect(Collectors.toMap(Profile::getUserId, Function.identity()));
		// 현재 재직 중 경력은 시작이 최신인 순으로 오므로 사용자마다 첫 건만 쓴다.
		Map<Long, String> companies = careerRepository.findByUserIdInAndIsCurrentTrueOrderByStartYmDescIdDesc(userIds)
				.stream()
				.collect(Collectors.toMap(Career::getUserId, Career::getCompany, (first, ignored) -> first));
		Map<Long, String> reviewerNames = findReviewerNames(content);
		List<UserIdentity> approved = userRepository.findApprovedIdentities(
				content.stream().map(User::getName).collect(Collectors.toSet()),
				content.stream().map(User::getGeneration).collect(Collectors.toSet()));

		return PageResponse.from(users, user -> {
			Profile profile = profiles.get(user.getId());
			return SignupListItemResponse.of(
					user,
					profile == null ? List.of() : profile.getLionParts(),
					profile == null ? null : profile.getStatus(),
					companies.get(user.getId()),
					hasSameNameGeneration(user, approved),
					user.getReviewedBy() == null ? null : reviewerNames.get(user.getReviewedBy()));
		});
	}

	/** ADMIN-02. */
	public PendingCountResponse getPendingCount() {
		return new PendingCountResponse(userRepository.countByStatusAndDeletedAtIsNull(UserStatus.PENDING));
	}

	// 대기는 가입이 오래된 순, 처리된 탭은 처리 시각이 최신인 순 (처리 정보가 없는 계정은 맨 뒤).
	private Sort sortOf(UserStatus status) {
		if (status == UserStatus.PENDING) {
			return Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
		}
		return Sort.by(Sort.Order.desc("reviewedAt").nullsLast(), Sort.Order.desc("id"));
	}

	private Map<Long, String> findReviewerNames(List<User> users) {
		Set<Long> reviewerIds = users.stream()
				.map(User::getReviewedBy)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		if (reviewerIds.isEmpty()) {
			return Map.of();
		}
		return userRepository.findAllById(reviewerIds).stream()
				.collect(Collectors.toMap(User::getId, User::getName));
	}

	// 이름과 기수를 각각 IN으로 넉넉히 읽어 왔으므로 쌍이 실제로 같은지는 여기서 거른다. 자기 자신은 제외.
	private boolean hasSameNameGeneration(User user, List<UserIdentity> approved) {
		return approved.stream().anyMatch(other -> !other.id().equals(user.getId())
				&& other.name().equals(user.getName())
				&& other.generation().equals(user.getGeneration()));
	}
}
