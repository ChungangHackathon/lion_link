package com.lionlink.backend.domain.profile.section.lionactivity;

import com.lionlink.backend.domain.profile.exception.ProfileErrorCode;
import com.lionlink.backend.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 가입 기수가 바뀌는 곳(AUTH-06·07, ADMIN-06)이 함께 쓴다. 호출한 쪽의 트랜잭션 안에서 실행된다. */
@Component
@RequiredArgsConstructor
public class DefaultLionActivitySyncer {

	private final LionActivityRepository lionActivityRepository;

	/**
	 * 가입 때 자동 생성된 멋사 활동 항목(isDefault)의 기수를 가입 기수에 맞춘다.
	 * 바뀔 기수에 이미 다른 멋사 활동이 있으면 합치지 않고 409 (api.md 확정 사항 3).
	 */
	public void sync(Long userId, Integer generation) {
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
