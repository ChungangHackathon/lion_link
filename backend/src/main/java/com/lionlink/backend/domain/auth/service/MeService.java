package com.lionlink.backend.domain.auth.service;

import com.lionlink.backend.domain.auth.dto.request.SignupInfoRequest;
import com.lionlink.backend.domain.auth.dto.response.MeResponse;
import com.lionlink.backend.domain.auth.event.SignupRequestedEvent;
import com.lionlink.backend.domain.auth.exception.AuthErrorCode;
import com.lionlink.backend.domain.profile.basic.entity.Profile;
import com.lionlink.backend.domain.profile.basic.repository.ProfileRepository;
import com.lionlink.backend.domain.profile.section.lionactivity.DefaultLionActivitySyncer;
import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.entity.UserRole;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.domain.user.event.UserWithdrawnEvent;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.CommonErrorCode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeService {

	private final UserRepository userRepository;
	private final ProfileRepository profileRepository;
	private final DefaultLionActivitySyncer defaultLionActivitySyncer;
	private final ApplicationEventPublisher eventPublisher;
	private final RefreshTokenService refreshTokenService;

	/** AUTH-05: 승인 대기·반려 회원도 부를 수 있다. 탈퇴한 회원의 남은 accessToken은 401. */
	public MeResponse getMe(Long userId) {
		return toResponse(findActiveUser(userId));
	}

	/** AUTH-06: 승인 대기 회원만 이름·기수를 고친다. */
	@Transactional
	public MeResponse updateSignupInfo(Long userId, SignupInfoRequest request) {
		User user = findActiveUser(userId);
		if (user.getStatus() == UserStatus.REJECTED) {
			throw new BusinessException(AuthErrorCode.NOT_PENDING);
		}
		if (user.getStatus() == UserStatus.APPROVED) {
			throw new BusinessException(AuthErrorCode.ALREADY_APPROVED);
		}

		defaultLionActivitySyncer.sync(userId, request.generation());
		user.updateSignupInfo(request.name().trim(), request.generation());
		return toResponse(user);
	}

	/** AUTH-07: 반려 회원이 이름·기수를 고쳐 다시 신청한다. 관리자에게 가입 승인 요청 알림이 다시 간다. */
	@Transactional
	public MeResponse reapply(Long userId, SignupInfoRequest request) {
		User user = findActiveUser(userId);
		if (user.getStatus() != UserStatus.REJECTED) {
			throw new BusinessException(AuthErrorCode.NOT_REJECTED);
		}

		defaultLionActivitySyncer.sync(userId, request.generation());
		user.reapply(request.name().trim(), request.generation());
		eventPublisher.publishEvent(new SignupRequestedEvent(user.getId(), user.getName(), user.getGeneration()));
		return toResponse(user);
	}

	/**
	 * AUTH-08: 탈퇴. 승인 대기·반려 회원도 가능하다. 관리자는 불가.
	 * 이 경로는 승인 검사 인터셉터를 건너뛰므로 이미 탈퇴한 회원은 여기서 직접 401로 막는다.
	 * 다른 영역의 정리(커피챗·채팅·커뮤니티)는 UserWithdrawnEvent 구독자가 같은 트랜잭션에서 한다.
	 */
	@Transactional
	public void withdraw(Long userId) {
		User user = findActiveUser(userId);
		if (user.getRole() == UserRole.ADMIN) {
			throw new BusinessException(AuthErrorCode.ADMIN_CANNOT_WITHDRAW);
		}
		user.withdraw(LocalDateTime.now());
		refreshTokenService.revokeAll(userId);
		eventPublisher.publishEvent(new UserWithdrawnEvent(userId));
	}

	/** AUTH-01·02·05·06·07의 user 응답. */
	public MeResponse toResponse(User user) {
		String photoUrl = profileRepository.findById(user.getId()).map(Profile::getPhotoUrl).orElse(null);
		return MeResponse.of(user, photoUrl);
	}

	private User findActiveUser(Long userId) {
		return userRepository.findById(userId)
				.filter(user -> user.getDeletedAt() == null)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHENTICATED));
	}
}
