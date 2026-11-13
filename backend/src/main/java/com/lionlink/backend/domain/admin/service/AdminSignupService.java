package com.lionlink.backend.domain.admin.service;

import com.lionlink.backend.domain.admin.dto.request.AdminSignupInfoRequest;
import com.lionlink.backend.domain.admin.dto.request.RejectReasonType;
import com.lionlink.backend.domain.admin.dto.request.RejectRequest;
import com.lionlink.backend.domain.admin.dto.response.AdminSignupInfoResponse;
import com.lionlink.backend.domain.admin.dto.response.ApproveBatchResponse;
import com.lionlink.backend.domain.admin.dto.response.ApproveResponse;
import com.lionlink.backend.domain.admin.dto.response.PendingCountResponse;
import com.lionlink.backend.domain.admin.dto.response.RejectResponse;
import com.lionlink.backend.domain.admin.dto.response.SignupListItemResponse;
import com.lionlink.backend.domain.admin.event.SignupApprovedEvent;
import com.lionlink.backend.domain.admin.exception.AdminErrorCode;
import com.lionlink.backend.domain.profile.section.lionactivity.DefaultLionActivitySyncer;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.CommonErrorCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import org.springframework.context.ApplicationEventPublisher;
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
	private final DefaultLionActivitySyncer defaultLionActivitySyncer;
	private final ApplicationEventPublisher eventPublisher;

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

	/** ADMIN-03. 반려됨 탭의 '승인으로 바꾸기'도 같다. */
	@Transactional
	public ApproveResponse approve(Long adminId, Long userId) {
		return ApproveResponse.of(approveOne(adminId, userId));
	}

	/**
	 * ADMIN-04. 승인 대기 회원만 반려한다. 알림·메일은 보내지 않는다.
	 */
	@Transactional
	public RejectResponse reject(Long adminId, Long userId, RejectRequest request) {
		String reason = resolveReason(request);
		int updated = userRepository.reject(userId, adminId, LocalDateTime.now(), reason);
		if (updated == 0) {
			throw notUpdated(userId);
		}
		return RejectResponse.of(userRepository.findById(userId).orElseThrow());
	}

	/**
	 * ADMIN-05. 한 명씩 ADMIN-03과 같은 규칙으로 처리한다. 실패는 예외가 아니라 failed에 담으므로
	 * (조건부 갱신이 0행이면 아무것도 바뀌지 않았다) 한 명이 실패해도 나머지는 같은 트랜잭션에서 승인된다.
	 */
	@Transactional
	public ApproveBatchResponse approveBatch(Long adminId, List<Long> userIds) {
		if (new HashSet<>(userIds).size() != userIds.size() || userIds.stream().anyMatch(Objects::isNull)) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
		}
		List<ApproveBatchResponse.Succeeded> succeeded = new ArrayList<>();
		List<ApproveBatchResponse.Failed> failed = new ArrayList<>();
		for (Long userId : userIds) {
			try {
				succeeded.add(ApproveBatchResponse.Succeeded.of(approveOne(adminId, userId)));
			} catch (BusinessException e) {
				failed.add(new ApproveBatchResponse.Failed(userId, e.getErrorCode().getCode()));
			}
		}
		return new ApproveBatchResponse(succeeded, failed);
	}

	/** ADMIN-06. 상태와 상관없이 고친다. 기수가 바뀌면 자동 생성 멋사 활동 기수도 같이 바꾼다. */
	@Transactional
	public AdminSignupInfoResponse updateSignupInfo(Long userId, AdminSignupInfoRequest request) {
		User user = userRepository.findById(userId)
				.filter(found -> found.getDeletedAt() == null)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));
		defaultLionActivitySyncer.sync(userId, request.generation());
		user.changeSignupInfoByAdmin(request.name().trim(), request.generation());
		return AdminSignupInfoResponse.of(user);
	}

	// 조건부 갱신이 1행을 바꿨을 때만 알림 이벤트를 낸다.
	private User approveOne(Long adminId, Long userId) {
		if (userRepository.approve(userId, adminId, LocalDateTime.now()) == 0) {
			throw notUpdated(userId);
		}
		User user = userRepository.findById(userId).orElseThrow();
		eventPublisher.publishEvent(new SignupApprovedEvent(user.getId(), user.getName()));
		return user;
	}

	// 갱신된 행이 없을 때: 없거나 탈퇴한 회원이면 404, 있는데 조건에 안 맞으면 이미 처리된 가입이라 409.
	private BusinessException notUpdated(Long userId) {
		boolean exists = userRepository.existsByIdAndDeletedAtIsNull(userId);
		return new BusinessException(exists ? AdminErrorCode.ALREADY_PROCESSED : CommonErrorCode.NOT_FOUND);
	}

	private String resolveReason(RejectRequest request) {
		if (request.reasonType() != RejectReasonType.ETC) {
			return request.reasonType().getFixedText();
		}
		String text = request.reasonText() == null ? "" : request.reasonText().trim();
		if (text.length() < 5 || text.length() > 100) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR, "기타 사유는 5~100자로 입력해 주세요.");
		}
		return text;
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
