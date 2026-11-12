package com.lionlink.backend.domain.auth.service;

import com.lionlink.backend.domain.auth.dto.request.SignupInfoRequest;
import com.lionlink.backend.domain.auth.dto.response.MeResponse;
import com.lionlink.backend.domain.auth.event.SignupRequestedEvent;
import com.lionlink.backend.domain.auth.exception.AuthErrorCode;
import com.lionlink.backend.domain.profile.basic.entity.Profile;
import com.lionlink.backend.domain.profile.basic.repository.ProfileRepository;
import com.lionlink.backend.domain.profile.exception.ProfileErrorCode;
import com.lionlink.backend.domain.profile.section.lionactivity.LionActivityRepository;
import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.CommonErrorCode;
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
	private final LionActivityRepository lionActivityRepository;
	private final ApplicationEventPublisher eventPublisher;

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

		syncDefaultLionActivity(userId, request.generation());
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

		syncDefaultLionActivity(userId, request.generation());
		user.reapply(request.name().trim(), request.generation());
		eventPublisher.publishEvent(new SignupRequestedEvent(user.getId(), user.getName(), user.getGeneration()));
		return toResponse(user);
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

	/**
	 * 가입 때 자동 생성된 멋사 활동 항목(isDefault)의 기수를 가입 기수에 맞춘다.
	 * 바뀔 기수에 이미 다른 멋사 활동이 있으면 합치지 않고 409 (api.md 확정 사항 3).
	 */
	private void syncDefaultLionActivity(Long userId, Integer generation) {
		lionActivityRepository.findByUserIdAndIsDefaultTrue(userId)
				.filter(activity -> !activity.getGeneration().equals(generation))
				.ifPresent(activity -> {
					if (lionActivityRepository.existsByUserIdAndGeneration(userId, generation)) {
						throw new BusinessException(ProfileErrorCode.DUPLICATE_ITEM);
					}
					activity.changeGeneration(generation);
				});
	}
}
