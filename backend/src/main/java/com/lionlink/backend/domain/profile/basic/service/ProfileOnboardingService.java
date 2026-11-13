package com.lionlink.backend.domain.profile.basic.service;

import com.lionlink.backend.domain.auth.dto.response.MeResponse;
import com.lionlink.backend.domain.auth.service.MeService;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeSetting;
import com.lionlink.backend.domain.coffeechat.entity.MeetingMode;
import com.lionlink.backend.domain.coffeechat.repository.CoffeeSettingRepository;
import com.lionlink.backend.domain.notification.entity.NotificationSetting;
import com.lionlink.backend.domain.notification.repository.NotificationSettingRepository;
import com.lionlink.backend.domain.profile.Interest;
import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.ProfileStatus;
import com.lionlink.backend.domain.profile.basic.dto.request.OnboardingRequest;
import com.lionlink.backend.domain.profile.basic.dto.request.OnboardingRequest.StaffActivity;
import com.lionlink.backend.domain.profile.basic.entity.LinkType;
import com.lionlink.backend.domain.profile.basic.entity.Profile;
import com.lionlink.backend.domain.profile.basic.entity.ProfileLink;
import com.lionlink.backend.domain.profile.basic.repository.ProfileRepository;
import com.lionlink.backend.domain.profile.exception.ProfileErrorCode;
import com.lionlink.backend.domain.profile.section.career.Career;
import com.lionlink.backend.domain.profile.section.career.CareerRepository;
import com.lionlink.backend.domain.profile.section.education.Education;
import com.lionlink.backend.domain.profile.section.education.EducationRepository;
import com.lionlink.backend.domain.profile.section.lionactivity.LionActivity;
import com.lionlink.backend.domain.profile.section.lionactivity.LionActivityRepository;
import com.lionlink.backend.domain.profile.section.lionactivity.LionRole;
import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.CommonErrorCode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileOnboardingService {

	private static final String DEFAULT_SCHOOL = "중앙대학교";

	private final UserRepository userRepository;
	private final ProfileRepository profileRepository;
	private final CareerRepository careerRepository;
	private final EducationRepository educationRepository;
	private final LionActivityRepository lionActivityRepository;
	private final CoffeeSettingRepository coffeeSettingRepository;
	private final NotificationSettingRepository notificationSettingRepository;
	private final MeService meService;

	/**
	 * PROFILE-01: 프로필과 자동 등록 항목을 한 트랜잭션으로 만든다. 중간에 실패하면 전부 롤백된다.
	 * 커피챗·알림 설정은 다른 도메인 엔티티의 기본값 그대로 만들기만 한다.
	 */
	@Transactional
	public MeResponse onboard(Long userId, OnboardingRequest request) {
		User user = userRepository.findById(userId)
				.filter(found -> found.getDeletedAt() == null)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHENTICATED));
		if (user.isProfileCompleted() || profileRepository.existsById(userId)) {
			throw new BusinessException(ProfileErrorCode.ALREADY_COMPLETED);
		}

		profileRepository.save(buildProfile(user, request));
		user.completeProfile();

		if (request.status() == ProfileStatus.EMPLOYED) {
			careerRepository.save(Career.builder()
					.userId(userId)
					.company(request.company().trim())
					.title(request.jobTitle().trim())
					.isCurrent(true)
					.build());
		}
		educationRepository.save(Education.builder().userId(userId).school(DEFAULT_SCHOOL).build());
		lionActivityRepository.saveAll(buildLionActivities(user, request));

		coffeeSettingRepository.save(CoffeeSetting.builder()
				.userId(userId)
				.modes(List.of(MeetingMode.ONLINE, MeetingMode.OFFLINE))
				.build());
		notificationSettingRepository.save(NotificationSetting.builder().userId(userId).build());

		return meService.toResponse(user);
	}

	private Profile buildProfile(User user, OnboardingRequest request) {
		List<LionPart> parts = request.lionParts();
		List<ProfileLink> links = request.link() == null ? null : List.of(toLink(request.link().trim()));
		return Profile.builder()
				.userId(user.getId())
				.lionPart1(parts.get(0))
				.lionPart2(parts.size() > 1 ? parts.get(1) : null)
				.status(request.status())
				.interests(request.interests().stream().map(Interest::name).toList())
				.headline(request.headline())
				.links(links)
				.contactEmail(user.getEmail())
				.build();
	}

	private ProfileLink toLink(String url) {
		return new ProfileLink(LinkType.detect(url).name(), url);
	}

	/** 가입 기수 항목(isDefault)은 운영진 목록에 같은 기수가 있으면 STAFF, 없으면 LION. 나머지 기수는 STAFF 항목으로 더한다. */
	private List<LionActivity> buildLionActivities(User user, OnboardingRequest request) {
		List<StaffActivity> staff = request.staffActivities() == null ? List.of() : request.staffActivities();
		boolean staffInJoinGeneration = staff.stream()
				.anyMatch(activity -> activity.generation().equals(user.getGeneration()));

		List<LionActivity> activities = new ArrayList<>();
		activities.add(lionActivity(user.getId(), user.getGeneration(),
				staffInJoinGeneration ? LionRole.STAFF : LionRole.LION, request.lionParts(), true));
		staff.stream()
				.filter(activity -> !activity.generation().equals(user.getGeneration()))
				.forEach(activity -> activities.add(
						lionActivity(user.getId(), activity.generation(), LionRole.STAFF, activity.parts(), false)));
		return activities;
	}

	private LionActivity lionActivity(Long userId, Integer generation, LionRole role, List<LionPart> parts,
			boolean isDefault) {
		return LionActivity.builder()
				.userId(userId)
				.generation(generation)
				.role(role)
				.part1(parts.get(0))
				.part2(parts.size() > 1 ? parts.get(1) : null)
				.isDefault(isDefault)
				.build();
	}
}
