package com.lionlink.backend.domain.notification.service;

import com.lionlink.backend.domain.notification.dto.request.NotificationSettingUpdateRequest;
import com.lionlink.backend.domain.notification.dto.response.NotificationSettingResponse;
import com.lionlink.backend.domain.notification.entity.EmailType;
import com.lionlink.backend.domain.notification.entity.NotificationSetting;
import com.lionlink.backend.domain.notification.exception.NotificationErrorCode;
import com.lionlink.backend.domain.notification.repository.NotificationSettingRepository;
import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationSettingService {

	private final NotificationSettingRepository notificationSettingRepository;
	private final UserRepository userRepository;

	/** 설정이 아직 없으면 기본값(모두 켜짐)으로 만들어 돌려준다. */
	public NotificationSettingResponse getSettings(Long userId) {
		checkAccess(userId);
		return NotificationSettingResponse.from(getOrCreate(userId));
	}

	/** 보낸 항목만 바꾼다. */
	public NotificationSettingResponse updateSettings(Long userId, NotificationSettingUpdateRequest request) {
		checkAccess(userId);
		NotificationSetting setting = getOrCreate(userId);
		setting.update(request.emailCoffeeChat(), request.emailMessageRequest(), request.emailSignupApproved());
		return NotificationSettingResponse.from(setting);
	}

	/** 메일의 '이 알림 그만 받기' 링크. 로그인 없이 토큰으로 설정을 찾는다. */
	public void unsubscribe(String token, EmailType type) {
		NotificationSetting setting = notificationSettingRepository.findByUnsubscribeToken(token)
				.orElseThrow(() -> new BusinessException(NotificationErrorCode.INVALID_UNSUBSCRIBE_TOKEN));
		setting.unsubscribe(type);
	}

	/** 메일을 보낼 때도 이 메서드로 수신 여부와 수신 거부 토큰을 얻는다. */
	public NotificationSetting getOrCreate(Long userId) {
		return notificationSettingRepository.findById(userId)
				.orElseGet(() -> notificationSettingRepository.save(NotificationSetting.builder().userId(userId).build()));
	}

	// 알림 설정은 승인 대기 회원도 쓰므로 승인 여부를 보는 공통 인터셉터에서 빠져 있다. 반려 회원은 여기서 막는다.
	private void checkAccess(Long userId) {
		User user = userRepository.findById(userId)
				.filter(found -> found.getDeletedAt() == null)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHENTICATED));
		if (user.getStatus() == UserStatus.REJECTED) {
			throw new BusinessException(CommonErrorCode.NOT_APPROVED);
		}
	}
}
