package com.lionlink.backend.domain.notification.service;

import com.lionlink.backend.domain.notification.dto.request.NotificationSettingUpdateRequest;
import com.lionlink.backend.domain.notification.dto.response.NotificationSettingResponse;
import com.lionlink.backend.domain.notification.entity.EmailType;
import com.lionlink.backend.domain.notification.entity.NotificationSetting;
import com.lionlink.backend.domain.notification.exception.NotificationErrorCode;
import com.lionlink.backend.domain.notification.repository.NotificationSettingRepository;
import com.lionlink.backend.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationSettingService {

	private final NotificationSettingRepository notificationSettingRepository;

	/** 설정이 아직 없으면 기본값(모두 켜짐)으로 만들어 돌려준다. */
	public NotificationSettingResponse getSettings(Long userId) {
		return NotificationSettingResponse.from(getOrCreate(userId));
	}

	/** 보낸 항목만 바꾼다. */
	public NotificationSettingResponse updateSettings(Long userId, NotificationSettingUpdateRequest request) {
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
}
