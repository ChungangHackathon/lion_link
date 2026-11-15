package com.lionlink.backend.domain.profile.section.career;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 재직 중 경력 1개를 만든다. 고용 형태·시작 년월은 비워 둔다.
 * 온보딩(PROFILE-01)과 기본 정보 수정(PROFILE-04)이 같이 쓴다. 호출한 쪽의 트랜잭션 안에서 실행된다.
 */
@Component
@RequiredArgsConstructor
public class CurrentCareerCreator {

	private final CareerRepository careerRepository;

	public Career create(Long userId, String company, String jobTitle) {
		return careerRepository.save(Career.builder()
				.userId(userId)
				.company(company.strip())
				.title(jobTitle.strip())
				.isCurrent(true)
				.build());
	}
}
