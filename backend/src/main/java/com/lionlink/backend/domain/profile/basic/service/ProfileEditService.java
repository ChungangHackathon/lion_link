package com.lionlink.backend.domain.profile.basic.service;

import com.lionlink.backend.domain.profile.Interest;
import com.lionlink.backend.domain.profile.basic.dto.request.BasicInfoUpdateRequest;
import com.lionlink.backend.domain.profile.basic.dto.request.BioUpdateRequest;
import com.lionlink.backend.domain.profile.basic.dto.request.ContactUpdateRequest;
import com.lionlink.backend.domain.profile.basic.dto.request.LinksUpdateRequest;
import com.lionlink.backend.domain.profile.basic.dto.request.SkillsUpdateRequest;
import com.lionlink.backend.domain.profile.basic.dto.request.VisibilityUpdateRequest;
import com.lionlink.backend.domain.profile.basic.dto.response.BasicInfoResponse;
import com.lionlink.backend.domain.profile.basic.dto.response.BioResponse;
import com.lionlink.backend.domain.profile.basic.dto.response.ContactResponse;
import com.lionlink.backend.domain.profile.basic.dto.response.LinksResponse;
import com.lionlink.backend.domain.profile.basic.dto.response.SkillsResponse;
import com.lionlink.backend.domain.profile.basic.dto.response.VisibilityResponse;
import com.lionlink.backend.domain.profile.basic.entity.Profile;
import com.lionlink.backend.domain.profile.basic.entity.ProfileLink;
import com.lionlink.backend.domain.profile.basic.repository.ProfileRepository;
import com.lionlink.backend.domain.profile.ProfileStatus;
import com.lionlink.backend.domain.profile.section.career.CareerRepository;
import com.lionlink.backend.domain.profile.section.career.CurrentCareerCreator;
import com.lionlink.backend.domain.profile.section.lionactivity.DefaultLionActivitySyncer;
import com.lionlink.backend.domain.profile.section.service.CareerService;
import com.lionlink.backend.domain.profile.section.service.SectionItemSupport;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.CommonErrorCode;
import com.lionlink.backend.global.error.FieldErrorResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PROFILE-04·06~10: 내 프로필의 한 부분을 보낸 값으로 통째로 바꾼다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileEditService {

	private final UserRepository userRepository;
	private final ProfileRepository profileRepository;
	private final DefaultLionActivitySyncer lionActivitySyncer;
	private final SectionItemSupport sectionItemSupport;
	private final CareerRepository careerRepository;
	private final CurrentCareerCreator currentCareerCreator;

	/**
	 * 재직 상태인데 재직 중 경력이 하나도 없으면 회사·직무로 경력 1개를 만든다(없으면 400, 경력 10개면 409).
	 * 이미 재직 경력이 있으면 company·jobTitle은 무시하고, 재직에서 다른 상태로 바꿔도 경력은 그대로다.
	 * 대표 파트는 가입 기수 멋사 활동의 파트와 함께 바뀐다(양방향 동기화).
	 * 경력 개수 검사가 경쟁하지 않도록 프로필 행을 잠근다.
	 */
	@Transactional
	public BasicInfoResponse updateBasic(Long userId, BasicInfoUpdateRequest request) {
		Profile profile = sectionItemSupport.lockProfile(userId);
		// 검증이 실패하면 프로필을 바꾸기 전에 던져서 아무것도 바뀌지 않게 한다.
		createCurrentCareerIfNeeded(userId, request);
		profile.updateBasic(
				blankToNull(request.headline()),
				blankToNull(request.major()),
				blankToNull(request.minor()),
				blankToNull(request.region()),
				request.status(),
				request.lionParts().get(0),
				request.interests().stream().map(Interest::name).toList());
		lionActivitySyncer.syncPartToDefaultActivity(userId, profile.getLionPart());
		return BasicInfoResponse.from(profile);
	}

	private void createCurrentCareerIfNeeded(Long userId, BasicInfoUpdateRequest request) {
		if (request.status() != ProfileStatus.EMPLOYED || careerRepository.existsByUserIdAndIsCurrentTrue(userId)) {
			return;
		}
		List<FieldErrorResponse> missing = new ArrayList<>();
		if (isBlank(request.company())) {
			missing.add(new FieldErrorResponse("company", "재직 중이면 회사명을 입력해 주세요."));
		}
		if (isBlank(request.jobTitle())) {
			missing.add(new FieldErrorResponse("jobTitle", "재직 중이면 직무를 입력해 주세요."));
		}
		if (!missing.isEmpty()) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR, missing);
		}
		sectionItemSupport.checkLimit(careerRepository, userId, CareerService.MAX_ITEMS);
		currentCareerCreator.create(userId, request.company(), request.jobTitle());
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
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

	/** 가입 이메일(users.email)은 건드리지 않는다. 전화번호가 비어 있으면 null로 저장한다. */
	@Transactional
	public ContactResponse updateContact(Long userId, ContactUpdateRequest request) {
		Profile profile = findMyProfile(userId);
		profile.updateContact(request.email(), blankToNull(request.phone()));
		return ContactResponse.from(profile);
	}

	/** 보낸 키만 바꾼다. 안 보낸 키는 그대로. */
	@Transactional
	public VisibilityResponse updateVisibility(Long userId, VisibilityUpdateRequest request) {
		Profile profile = findMyProfile(userId);
		profile.updateVisibility(request.email(), request.phone(), request.links(), request.company());
		return VisibilityResponse.from(profile);
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
