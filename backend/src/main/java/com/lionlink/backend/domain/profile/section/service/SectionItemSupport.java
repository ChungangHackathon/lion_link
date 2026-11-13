package com.lionlink.backend.domain.profile.section.service;

import com.lionlink.backend.domain.profile.basic.repository.ProfileRepository;
import com.lionlink.backend.domain.profile.exception.ProfileErrorCode;
import com.lionlink.backend.domain.profile.section.ProfileSectionItem;
import com.lionlink.backend.domain.profile.section.SectionItemRepository;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 목록형 섹션 서비스(PROFILE-11)가 함께 쓰는 공통 처리. 호출한 서비스의 트랜잭션 안에서 실행된다. */
@Component
@RequiredArgsConstructor
public class SectionItemSupport {

	private final UserRepository userRepository;
	private final ProfileRepository profileRepository;

	/**
	 * 내 프로필 행을 잠가 같은 회원의 섹션 변경을 한 줄로 세운다.
	 * 동시에 두 요청이 와도 최대 개수·중복 검사를 둘 다 통과하는 일이 없다.
	 * 허용 경로는 인터셉터가 탈퇴 여부를 보지 않을 수 있어 직접 확인한다.
	 */
	public void lockProfile(Long userId) {
		if (!userRepository.existsByIdAndDeletedAtIsNull(userId)) {
			throw new BusinessException(CommonErrorCode.UNAUTHENTICATED);
		}
		profileRepository.findByIdForUpdate(userId)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.PROFILE_NOT_COMPLETED));
	}

	/** 없는 항목과 남의 항목은 구분하지 않고 둘 다 404. */
	public <T extends ProfileSectionItem> T findOwned(SectionItemRepository<T> repository, Long itemId, Long userId) {
		return repository.findByIdAndUserId(itemId, userId)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));
	}

	public void checkLimit(SectionItemRepository<?> repository, Long userId, int max) {
		if (repository.countByUserId(userId) >= max) {
			throw new BusinessException(ProfileErrorCode.LIMIT_EXCEEDED);
		}
	}

	/** 앞뒤 공백을 지우고, 비었으면 null. */
	public static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.strip();
	}

	public static String strip(String value) {
		return value == null ? null : value.strip();
	}
}
