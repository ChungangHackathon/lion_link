package com.lionlink.backend.domain.profile.basic.service;

import com.lionlink.backend.domain.profile.Interest;
import com.lionlink.backend.domain.profile.basic.dto.request.BasicInfoUpdateRequest;
import com.lionlink.backend.domain.profile.basic.dto.request.BioUpdateRequest;
import com.lionlink.backend.domain.profile.basic.dto.request.LinksUpdateRequest;
import com.lionlink.backend.domain.profile.basic.dto.request.SkillsUpdateRequest;
import com.lionlink.backend.domain.profile.basic.dto.response.BasicInfoResponse;
import com.lionlink.backend.domain.profile.basic.dto.response.BioResponse;
import com.lionlink.backend.domain.profile.basic.dto.response.LinksResponse;
import com.lionlink.backend.domain.profile.basic.dto.response.SkillsResponse;
import com.lionlink.backend.domain.profile.basic.entity.Profile;
import com.lionlink.backend.domain.profile.basic.entity.ProfileLink;
import com.lionlink.backend.domain.profile.basic.repository.ProfileRepository;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.CommonErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PROFILE-04·06·07·08: 내 프로필의 한 부분을 보낸 값으로 통째로 바꾼다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileEditService {

	private final UserRepository userRepository;
	private final ProfileRepository profileRepository;

	/** 현재 상태가 바뀌어도 경력은 건드리지 않고, 자동 생성 멋사 활동의 파트도 따로 둔다. */
	@Transactional
	public BasicInfoResponse updateBasic(Long userId, BasicInfoUpdateRequest request) {
		Profile profile = findMyProfile(userId);
		profile.updateBasic(
				blankToNull(request.headline()),
				blankToNull(request.major()),
				blankToNull(request.minor()),
				blankToNull(request.region()),
				request.status(),
				request.lionParts(),
				request.interests().stream().map(Interest::name).toList());
		return BasicInfoResponse.from(profile);
	}

	@Transactional
	public BioResponse updateBio(Long userId, BioUpdateRequest request) {
		Profile profile = findMyProfile(userId);
		profile.updateBio(blankToNull(request.bio()));
		return BioResponse.from(profile);
	}

	/** 앞뒤 공백을 지우고 같은 기술은 앞쪽만 남긴다. 순서는 보낸 그대로. */
	@Transactional
	public SkillsResponse updateSkills(Long userId, SkillsUpdateRequest request) {
		Profile profile = findMyProfile(userId);
		List<String> skills = request.skills() == null ? List.of() : request.skills().stream()
				.map(String::strip)
				.distinct()
				.toList();
		profile.updateSkills(skills);
		return SkillsResponse.from(profile);
	}

	@Transactional
	public LinksResponse updateLinks(Long userId, LinksUpdateRequest request) {
		Profile profile = findMyProfile(userId);
		List<ProfileLink> links = request.links() == null ? List.of() : request.links().stream()
				.map(link -> new ProfileLink(link.type().name(), link.url().strip()))
				.toList();
		profile.updateLinks(links);
		return LinksResponse.from(profile);
	}

	/** 허용 경로는 인터셉터가 탈퇴 여부를 보지 않을 수 있어 직접 확인한다. 프로필이 없으면 PROFILE_NOT_COMPLETED. */
	private Profile findMyProfile(Long userId) {
		if (!userRepository.existsByIdAndDeletedAtIsNull(userId)) {
			throw new BusinessException(CommonErrorCode.UNAUTHENTICATED);
		}
		return profileRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.PROFILE_NOT_COMPLETED));
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.strip();
	}
}
