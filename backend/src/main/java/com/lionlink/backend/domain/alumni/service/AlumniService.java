package com.lionlink.backend.domain.alumni.service;

import com.lionlink.backend.domain.alumni.dto.request.AlumniSearchCondition;
import com.lionlink.backend.domain.alumni.dto.response.AlumniResponse;
import com.lionlink.backend.domain.alumni.dto.response.BookmarkedAlumniResponse;
import com.lionlink.backend.domain.alumni.exception.AlumniErrorCode;
import com.lionlink.backend.domain.alumni.repository.AlumniSearchRepository;
import com.lionlink.backend.domain.alumni.repository.BookmarkRepository;
import com.lionlink.backend.domain.alumni.repository.RelationQueryRepository;
import com.lionlink.backend.domain.profile.CoffeeAvailability;
import com.lionlink.backend.domain.profile.CoffeeAvailabilityReader;
import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.basic.entity.Profile;
import com.lionlink.backend.domain.profile.basic.repository.ProfileRepository;
import com.lionlink.backend.domain.profile.section.career.Career;
import com.lionlink.backend.domain.profile.section.career.CareerRepository;
import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.response.PageResponse;
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

	private final AlumniSearchRepository alumniSearchRepository;
	private final BookmarkRepository bookmarkRepository;
	private final UserRepository userRepository;
	private final ProfileRepository profileRepository;
	private final CareerRepository careerRepository;
	private final CoffeeAvailabilityReader coffeeAvailabilityReader;
	private final RelationQueryRepository relationQueryRepository;

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
		Set<Long> connectedIds = relationQueryRepository.findConnectedIds(userId);
		Map<Long, String> companies = currentCompanies(userIds);
		Map<Long, CoffeeAvailability> availabilities = coffeeAvailabilityReader.getAvailabilities(userIds);

		return row -> {
			boolean companyVisible = row.profile().getCompanyVisibility()
					.isVisibleTo(row.id().equals(userId), connectedIds.contains(row.id()));
			return AlumniResponse.of(row, companyVisible ? companies.get(row.id()) : null, bookmarkedIds.contains(row.id()),
					availabilities.get(row.id()));
		};
	}

	private Map<Long, String> currentCompanies(List<Long> userIds) {
		Map<Long, String> companies = new HashMap<>();
		for (Career career : careerRepository.findByUserIdInAndIsCurrentTrueOrderByStartYmDescIdDesc(userIds)) {
			companies.putIfAbsent(career.getUserId(), career.getCompany());
		}
		return companies;
	}
}
