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
import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.response.PageResponse;
import java.util.ArrayList;
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
		Profile profile = profileRepository.findById(userId).orElse(null);
		LionPart part = profile == null ? null : profile.getLionPart();
		List<String> interests = profile == null ? List.of() : profile.getInterests();

		List<AlumniSearchRepository.Row> rows = new ArrayList<>(
				alumniSearchRepository.findSeniors(userId, user.getGeneration(), part, interests, limit));
		if (rows.size() < limit) {
			List<Long> excludedIds = new ArrayList<>(rows.stream().map(AlumniSearchRepository.Row::id).toList());
			excludedIds.add(userId);
			rows.addAll(alumniSearchRepository.findRandom(userId, excludedIds, limit - rows.size()));
		}
		return rows.stream().map(toResponse(userId, rows)).toList();
	}

	private Function<AlumniSearchRepository.Row, AlumniResponse> toResponse(Long userId, List<AlumniSearchRepository.Row> rows) {
		List<Long> userIds = rows.stream().map(AlumniSearchRepository.Row::id).toList();
		if (userIds.isEmpty()) {
			return row -> AlumniResponse.of(row, null, null, false, CoffeeAvailability.NOT_ACCEPTING);
		}

		Set<Long> bookmarkedIds = bookmarkRepository.findTargetUserIds(userId, userIds);
		Set<Long> connectedIds = relationQueryRepository.findConnectedIds(userId);
		Map<Long, CoffeeAvailability> availabilities = coffeeAvailabilityReader.getAvailabilities(userIds);

		// 회사는 프로필에 저장된 값이고 공개 범위를 적용한다. 직무는 항상 보여 준다.
		return row -> {
			Profile profile = row.profile();
			boolean companyVisible = profile.getStatus().isWorking()
					&& profile.getCompanyVisibility()
					.isVisibleTo(row.id().equals(userId), connectedIds.contains(row.id()));
			return AlumniResponse.of(row, companyVisible ? profile.getCompany() : null, profile.getJobTitle(),
					bookmarkedIds.contains(row.id()), availabilities.get(row.id()));
		};
	}
}
