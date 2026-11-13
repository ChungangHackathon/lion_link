package com.lionlink.backend.domain.alumni.service;

import com.lionlink.backend.domain.alumni.dto.response.AlumniDetailResponse;
import com.lionlink.backend.domain.alumni.dto.response.AlumniDetailResponse.Chat;
import com.lionlink.backend.domain.alumni.dto.response.AlumniDetailResponse.ChatStatus;
import com.lionlink.backend.domain.alumni.dto.response.AlumniDetailResponse.CoffeeChat;
import com.lionlink.backend.domain.alumni.dto.response.AlumniDetailResponse.CoffeeChatAvailability;
import com.lionlink.backend.domain.alumni.dto.response.AlumniDetailResponse.Commonality;
import com.lionlink.backend.domain.alumni.dto.response.AlumniDetailResponse.CommonalityType;
import com.lionlink.backend.domain.alumni.dto.response.AlumniDetailResponse.Sections;
import com.lionlink.backend.domain.alumni.entity.BookmarkId;
import com.lionlink.backend.domain.alumni.exception.AlumniErrorCode;
import com.lionlink.backend.domain.alumni.repository.BookmarkRepository;
import com.lionlink.backend.domain.alumni.repository.RelationQueryRepository;
import com.lionlink.backend.domain.chat.entity.ChatRoom;
import com.lionlink.backend.domain.chat.entity.ChatRoomStatus;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeSetting;
import com.lionlink.backend.domain.coffeechat.repository.CoffeeSettingRepository;
import com.lionlink.backend.domain.coffeechat.service.CoffeeChatCooldownPolicy;
import com.lionlink.backend.domain.profile.CoffeeAvailability;
import com.lionlink.backend.domain.profile.CoffeeAvailabilityReader;
import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.basic.entity.Profile;
import com.lionlink.backend.domain.profile.basic.repository.ProfileRepository;
import com.lionlink.backend.domain.profile.section.activity.ActivityRepository;
import com.lionlink.backend.domain.profile.section.award.AwardRepository;
import com.lionlink.backend.domain.profile.section.career.Career;
import com.lionlink.backend.domain.profile.section.career.CareerRepository;
import com.lionlink.backend.domain.profile.section.certification.CertificationRepository;
import com.lionlink.backend.domain.profile.section.education.EducationRepository;
import com.lionlink.backend.domain.profile.section.language.LanguageRepository;
import com.lionlink.backend.domain.profile.section.lionactivity.LionActivityRepository;
import com.lionlink.backend.domain.profile.section.lionactivity.LionRole;
import com.lionlink.backend.domain.profile.section.lionproject.LionProject;
import com.lionlink.backend.domain.profile.section.lionproject.LionProjectRepository;
import com.lionlink.backend.domain.profile.section.project.ProjectRepository;
import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.error.BusinessException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlumniDetailService {

	private static final Set<LionRole> STAFF_ROLES = EnumSet.of(LionRole.STAFF, LionRole.PRESIDENT, LionRole.PART_LEAD);
	private static final int CHAT_REQUEST_WAIT_DAYS = 7;
	private static final Map<LionPart, String> PART_LABELS = Map.of(
			LionPart.PLANNING, "기획",
			LionPart.DESIGN, "디자인",
			LionPart.FRONTEND, "프론트엔드",
			LionPart.BACKEND, "백엔드"
	);
	private static final Map<String, String> INTEREST_LABELS = Map.of(
			"PLANNING_PM", "기획·PM",
			"DESIGN", "디자인",
			"FRONTEND", "프론트엔드",
			"BACKEND", "백엔드",
			"APP", "앱",
			"DATA_AI", "데이터·AI",
			"INFRA_DEVOPS", "인프라·DevOps",
			"MARKETING", "마케팅",
			"STARTUP", "창업",
			"ETC", "기타"
	);

	private final UserRepository userRepository;
	private final ProfileRepository profileRepository;
	private final CareerRepository careerRepository;
	private final EducationRepository educationRepository;
	private final LionActivityRepository lionActivityRepository;
	private final LionProjectRepository lionProjectRepository;
	private final ProjectRepository projectRepository;
	private final CertificationRepository certificationRepository;
	private final AwardRepository awardRepository;
	private final ActivityRepository activityRepository;
	private final LanguageRepository languageRepository;
	private final BookmarkRepository bookmarkRepository;
	private final CoffeeSettingRepository coffeeSettingRepository;
	private final CoffeeAvailabilityReader coffeeAvailabilityReader;
	private final RelationQueryRepository relationQueryRepository;
	private final CoffeeChatCooldownPolicy cooldownPolicy;

	public AlumniDetailResponse getDetail(Long userId, Long targetUserId) {
		User target = userRepository.findById(targetUserId)
				.filter(user -> user.getStatus() == UserStatus.APPROVED && user.getDeletedAt() == null)
				.orElseThrow(() -> new BusinessException(AlumniErrorCode.USER_NOT_FOUND));
		Profile profile = profileRepository.findById(targetUserId)
				.orElseThrow(() -> new BusinessException(AlumniErrorCode.USER_NOT_FOUND));

		boolean owner = userId.equals(targetUserId);
		Set<Long> myConnections = relationQueryRepository.findConnectedIds(userId);
		boolean connected = myConnections.contains(targetUserId);
		AlumniDetailResponse.Visibility visibility = new AlumniDetailResponse.Visibility(
				profile.getEmailVisibility().isVisibleTo(owner, connected),
				profile.getPhoneVisibility().isVisibleTo(owner, connected),
				profile.getLinksVisibility().isVisibleTo(owner, connected),
				profile.getCompanyVisibility().isVisibleTo(owner, connected)
		);
		Sections sections = sections(targetUserId);

		AlumniDetailResponse.Relation relation = new AlumniDetailResponse.Relation(
				sections.lionActivities().stream().anyMatch(activity -> STAFF_ROLES.contains(activity.getRole())),
				coffeeChat(userId, targetUserId, owner),
				owner ? new Chat(ChatStatus.NONE, null) : chat(userId, targetUserId),
				owner ? List.of() : commonalities(userId, target, profile, sections, visibility.company()),
				owner ? 0 : mutualCount(userId, targetUserId, myConnections),
				!owner && bookmarkRepository.existsById(new BookmarkId(userId, targetUserId))
		);
		return AlumniDetailResponse.of(target, profile, sections, visibility, relation);
	}

	private Sections sections(Long userId) {
		return new Sections(
				careerRepository.findByUserIdOrderByIsCurrentDescStartYmDescIdDesc(userId),
				educationRepository.findByUserIdOrderByIsCurrentDescStartYmDescIdDesc(userId),
				lionActivityRepository.findByUserIdOrderByGenerationDesc(userId),
				lionProjectRepository.findByUserIdOrderByGenerationDescIdDesc(userId),
				projectRepository.findByUserIdOrderByIsCurrentDescStartYmDescIdDesc(userId),
				certificationRepository.findByUserIdOrderByDateDescIdDesc(userId),
				awardRepository.findByUserIdOrderByDateDescIdDesc(userId),
				activityRepository.findByUserIdOrderByStartYmDescIdDesc(userId),
				languageRepository.findByUserIdOrderByIdAsc(userId)
		);
	}

	private CoffeeChat coffeeChat(Long userId, Long targetUserId, boolean owner) {
		CoffeeSetting setting = coffeeSettingRepository.findById(targetUserId).orElse(null);
		CoffeeAvailability availability = coffeeAvailabilityReader.getAvailability(targetUserId);
		boolean inProgress = !owner && relationQueryRepository.existsInProgress(userId, targetUserId);
		Optional<LocalDate> cooldown = owner || inProgress
				? Optional.empty()
				: cooldownPolicy.availableFrom(userId, targetUserId, LocalDate.now());
		CoffeeChatAvailability status = inProgress
				? CoffeeChatAvailability.IN_PROGRESS
				: cooldown.isPresent() ? CoffeeChatAvailability.COOLDOWN : CoffeeChatAvailability.valueOf(availability.availability().name());
		return new CoffeeChat(
				status,
				inProgress ? null : cooldown.orElse(availability.nextAvailableDate()),
				availability.acceptGroup(),
				setting == null ? List.of() : Objects.requireNonNullElse(setting.getTopics(), List.of()),
				setting == null ? List.of() : Objects.requireNonNullElse(setting.getModes(), List.of()),
				setting == null ? List.of() : Objects.requireNonNullElse(setting.getAvailableSlots(), List.of()),
				setting == null ? null : setting.getPreferredArea(),
				owner ? null : relationQueryRepository.findPendingCoffeeChatId(userId, targetUserId).orElse(null)
		);
	}

	private Chat chat(Long userId, Long targetUserId) {
		ChatRoom room = relationQueryRepository.findDirectRoom(userId, targetUserId).orElse(null);
		if (room == null) {
			return new Chat(ChatStatus.NONE, null);
		}
		if (room.getStatus() == ChatRoomStatus.ACTIVE) {
			return new Chat(ChatStatus.ACTIVE, null);
		}
		if (room.getStatus() == ChatRoomStatus.REQUESTED) {
			return new Chat(ChatStatus.REQUESTED, null);
		}
		if (room.getStatus() == ChatRoomStatus.DECLINED && userId.equals(room.getRequestedBy()) && room.getDeclinedAt() != null) {
			LocalDateTime availableAt = room.getDeclinedAt().plusDays(CHAT_REQUEST_WAIT_DAYS);
			if (availableAt.isAfter(LocalDateTime.now())) {
				return new Chat(ChatStatus.NONE, availableAt.toLocalDate());
			}
		}
		return new Chat(ChatStatus.NONE, null);
	}

	private List<Commonality> commonalities(Long userId, User target, Profile targetProfile, Sections targetSections,
			boolean companyVisible) {
		User me = userRepository.findById(userId).orElseThrow();
		Profile myProfile = profileRepository.findById(userId).orElse(null);
		List<Commonality> result = new ArrayList<>();

		Set<String> myProjects = lionProjectRepository.findByUserIdOrderByGenerationDescIdDesc(userId).stream()
				.map(this::projectKey)
				.collect(Collectors.toSet());
		targetSections.lionProjects().stream()
				.filter(project -> myProjects.contains(projectKey(project)))
				.map(project -> project.getGeneration() + "기 " + project.getName())
				.distinct()
				.forEach(label -> result.add(new Commonality(CommonalityType.SAME_LION_PROJECT, label)));

		if (companyVisible) {
			Set<String> myCompanies = currentCompanies(careerRepository.findByUserIdOrderByIsCurrentDescStartYmDescIdDesc(userId)).stream()
					.map(this::normalize)
					.collect(Collectors.toSet());
			currentCompanies(targetSections.careers()).stream()
					.filter(company -> myCompanies.contains(normalize(company)))
					.forEach(company -> result.add(new Commonality(CommonalityType.SAME_COMPANY, company)));
		}

		if (Objects.equals(me.getGeneration(), target.getGeneration())) {
			result.add(new Commonality(CommonalityType.SAME_GENERATION, target.getGeneration() + "기"));
		}

		List<LionPart> myParts = myProfile == null ? List.of() : myProfile.getLionParts();
		targetProfile.getLionParts().stream()
				.filter(myParts::contains)
				.forEach(part -> result.add(new Commonality(CommonalityType.SAME_PART, PART_LABELS.getOrDefault(part, part.name()) + " 파트")));

		List<String> myInterests = myProfile == null ? List.of() : Objects.requireNonNullElse(myProfile.getInterests(), List.of());
		Objects.requireNonNullElse(targetProfile.getInterests(), List.<String>of()).stream()
				.filter(myInterests::contains)
				.forEach(interest -> result.add(
						new Commonality(CommonalityType.SHARED_INTEREST, INTEREST_LABELS.getOrDefault(interest, interest))));
		return result;
	}

	private long mutualCount(Long userId, Long targetUserId, Set<Long> myConnections) {
		Set<Long> mutual = new HashSet<>(myConnections);
		mutual.retainAll(relationQueryRepository.findConnectedIds(targetUserId));
		mutual.remove(userId);
		mutual.remove(targetUserId);
		if (mutual.isEmpty()) {
			return 0;
		}
		return userRepository.findIdsByStatus(mutual, UserStatus.APPROVED).size();
	}

	private Set<String> currentCompanies(List<Career> careers) {
		return careers.stream()
				.filter(Career::isCurrent)
				.map(Career::getCompany)
				.filter(company -> company != null && !company.isBlank())
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private String projectKey(LionProject project) {
		return project.getGeneration() + ":" + normalize(project.getName());
	}

	private String normalize(String value) {
		return value.trim().toLowerCase(Locale.ROOT);
	}
}
