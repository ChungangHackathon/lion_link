package com.lionlink.backend.domain.alumni.service;

import com.lionlink.backend.domain.alumni.dto.request.AlumniSearchCondition;
import com.lionlink.backend.domain.alumni.dto.response.AlumniDetailResponse;
import com.lionlink.backend.domain.alumni.dto.response.AlumniResponse;
import com.lionlink.backend.domain.alumni.dto.response.BookmarkedAlumniResponse;
import com.lionlink.backend.domain.alumni.entity.BookmarkId;
import com.lionlink.backend.domain.alumni.exception.AlumniErrorCode;
import com.lionlink.backend.domain.alumni.repository.AlumniSearchRepository;
import com.lionlink.backend.domain.alumni.repository.BookmarkRepository;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatStatus;
import com.lionlink.backend.domain.coffeechat.repository.CoffeeChatRepository;
import com.lionlink.backend.domain.profile.CoffeeAvailability;
import com.lionlink.backend.domain.profile.CoffeeAvailabilityReader;
import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.basic.entity.Profile;
import com.lionlink.backend.domain.profile.basic.repository.ProfileRepository;
import com.lionlink.backend.domain.profile.section.career.Career;
import com.lionlink.backend.domain.profile.section.career.CareerRepository;
import com.lionlink.backend.domain.profile.section.education.EducationRepository;
import com.lionlink.backend.domain.profile.section.lionactivity.LionActivityRepository;
import com.lionlink.backend.domain.profile.section.project.ProjectRepository;
import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.response.PageResponse;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlumniService {

	private static final Set<CoffeeChatStatus> CONNECTED_STATUSES =
			EnumSet.of(CoffeeChatStatus.ACCEPTED, CoffeeChatStatus.SCHEDULED, CoffeeChatStatus.COMPLETED);

	private final AlumniSearchRepository alumniSearchRepository;
	private final BookmarkRepository bookmarkRepository;
	private final UserRepository userRepository;
	private final ProfileRepository profileRepository;
	private final CareerRepository careerRepository;
	private final EducationRepository educationRepository;
	private final ProjectRepository projectRepository;
	private final LionActivityRepository lionActivityRepository;
	private final CoffeeChatRepository coffeeChatRepository;
	private final CoffeeAvailabilityReader coffeeAvailabilityReader;

	public PageResponse<AlumniResponse> search(Long userId, AlumniSearchCondition condition, Pageable pageable) {
		List<LionPart> userParts = profileRepository.findById(userId).map(Profile::getLionParts).orElse(List.of());
		Page<AlumniSearchRepository.Row> rows = alumniSearchRepository.search(userId, userParts, condition, pageable);
		return PageResponse.from(rows, toResponse(userId, rows.getContent()));
	}

	public PageResponse<BookmarkedAlumniResponse> getBookmarks(Long userId, Pageable pageable) {
		Page<AlumniSearchRepository.BookmarkedRow> rows = alumniSearchRepository.findBookmarked(userId, pageable);
		Function<AlumniSearchRepository.Row, AlumniResponse> toResponse =
				toResponse(userId, rows.map(AlumniSearchRepository.BookmarkedRow::row).getContent());
		return PageResponse.from(rows, row -> new BookmarkedAlumniResponse(toResponse.apply(row.row()), row.bookmarkedAt()));
	}

	public long countBookmarks(Long userId) {
		return alumniSearchRepository.countBookmarked(userId);
	}

	public List<AlumniResponse> recommendSeniors(Long userId, int limit) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(AlumniErrorCode.USER_NOT_FOUND));
		LionPart part = profileRepository.findById(userId).map(Profile::getLionPart1).orElse(null);
		List<AlumniSearchRepository.Row> rows = alumniSearchRepository.findSeniors(userId, user.getGeneration(), part, limit);
		return rows.stream().map(toResponse(userId, rows)).toList();
	}

	private Function<AlumniSearchRepository.Row, AlumniResponse> toResponse(Long userId, List<AlumniSearchRepository.Row> rows) {
		List<Long> userIds = rows.stream().map(AlumniSearchRepository.Row::id).toList();
		if (userIds.isEmpty()) {
			return row -> AlumniResponse.of(row, null, false, CoffeeAvailability.NOT_ACCEPTING);
		}

		Set<Long> bookmarkedIds = bookmarkRepository.findTargetUserIds(userId, userIds);
		Set<Long> connectedIds = coffeeChatRepository.findPartnerIds(userId, userIds, CONNECTED_STATUSES);
		Map<Long, String> companies = currentCompanies(userIds);
		Map<Long, CoffeeAvailability> availabilities = coffeeAvailabilityReader.getAvailabilities(userIds);

		return row -> {
			boolean companyVisible = row.profile().getCompanyVisibility()
					.isVisibleTo(row.id().equals(userId), connectedIds.contains(row.id()));
			return AlumniResponse.of(row, companyVisible ? companies.get(row.id()) : null, bookmarkedIds.contains(row.id()),
					availabilities.get(row.id()));
		};
	}

	public AlumniDetailResponse getDetail(Long userId, Long targetUserId) {
		User user = userRepository.findById(targetUserId)
				.filter(target -> target.getStatus() == UserStatus.APPROVED && target.getDeletedAt() == null)
				.orElseThrow(() -> new BusinessException(AlumniErrorCode.USER_NOT_FOUND));
		Profile profile = profileRepository.findById(targetUserId)
				.orElseThrow(() -> new BusinessException(AlumniErrorCode.USER_NOT_FOUND));

		boolean owner = userId.equals(targetUserId);
		AlumniDetailResponse.Viewer viewer = owner
				? new AlumniDetailResponse.Viewer(true, false, false, false)
				: new AlumniDetailResponse.Viewer(
						false,
						!coffeeChatRepository.findPartnerIds(userId, List.of(targetUserId), CONNECTED_STATUSES).isEmpty(),
						bookmarkRepository.existsById(new BookmarkId(userId, targetUserId)),
						isCoffeeChatAvailable(userId, targetUserId)
				);

		AlumniDetailResponse.Sections sections = new AlumniDetailResponse.Sections(
				careerRepository.findByUserIdOrderByIsCurrentDescStartYmDescIdDesc(targetUserId),
				educationRepository.findByUserIdOrderByIsCurrentDescStartYmDescIdDesc(targetUserId),
				projectRepository.findByUserIdOrderByIsCurrentDescStartYmDescIdDesc(targetUserId),
				lionActivityRepository.findByUserIdOrderByGenerationDesc(targetUserId)
		);
		return AlumniDetailResponse.of(user, profile, sections, viewer);
	}

	private Map<Long, String> currentCompanies(List<Long> userIds) {
		Map<Long, String> companies = new HashMap<>();
		for (Career career : careerRepository.findByUserIdInAndIsCurrentTrueOrderByStartYmDescIdDesc(userIds)) {
			companies.putIfAbsent(career.getUserId(), career.getCompany());
		}
		return companies;
	}

	private boolean isCoffeeChatAvailable(Long userId, Long targetUserId) {
		return coffeeAvailabilityReader.getAvailability(targetUserId).available()
				&& !coffeeChatRepository.existsOpenChat(userId, targetUserId);
	}
}
