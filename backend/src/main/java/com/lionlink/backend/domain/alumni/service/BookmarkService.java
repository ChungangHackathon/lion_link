package com.lionlink.backend.domain.alumni.service;

import com.lionlink.backend.domain.alumni.dto.response.BookmarkResponse;
import com.lionlink.backend.domain.alumni.dto.response.BookmarkedAlumniResponse;
import com.lionlink.backend.domain.alumni.entity.Bookmark;
import com.lionlink.backend.domain.alumni.entity.BookmarkId;
import com.lionlink.backend.domain.alumni.exception.AlumniErrorCode;
import com.lionlink.backend.domain.alumni.repository.BookmarkRepository;
import com.lionlink.backend.domain.profile.UserSummary;
import com.lionlink.backend.domain.profile.UserSummaryReader;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.response.SliceResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookmarkService {

	private final BookmarkRepository bookmarkRepository;
	private final UserRepository userRepository;
	private final UserSummaryReader userSummaryReader;

	public BookmarkResponse bookmark(Long userId, Long targetUserId) {
		if (userId.equals(targetUserId)) {
			throw new BusinessException(AlumniErrorCode.CANNOT_BOOKMARK_SELF);
		}
		BookmarkId id = new BookmarkId(userId, targetUserId);
		if (bookmarkRepository.existsById(id)) {
			return BookmarkResponse.bookmarked(targetUserId);
		}
		if (!userRepository.existsByIdAndStatusAndDeletedAtIsNull(targetUserId, UserStatus.APPROVED)) {
			throw new BusinessException(AlumniErrorCode.USER_NOT_FOUND);
		}
		try {
			bookmarkRepository.saveAndFlush(Bookmark.builder()
					.userId(userId)
					.targetUserId(targetUserId)
					.build());
		} catch (DataIntegrityViolationException e) {
			if (!bookmarkRepository.existsById(id)) {
				throw e;
			}
		}
		return BookmarkResponse.bookmarked(targetUserId);
	}

	@Transactional
	public void unbookmark(Long userId, Long targetUserId) {
		bookmarkRepository.deleteById(new BookmarkId(userId, targetUserId));
	}

	@Transactional(readOnly = true)
	public SliceResponse<BookmarkedAlumniResponse> getBookmarks(Long userId, Pageable pageable) {
		Slice<Bookmark> bookmarks = bookmarkRepository.findByUserIdOrderByCreatedAtDescTargetUserIdDesc(userId, pageable);
		Map<Long, UserSummary> summaries = userSummaryReader.getSummaries(
				bookmarks.map(Bookmark::getTargetUserId).getContent());
		return SliceResponse.from(bookmarks, bookmark -> BookmarkedAlumniResponse.of(
				bookmark,
				summaries.getOrDefault(bookmark.getTargetUserId(), UserSummary.withdrawn(bookmark.getTargetUserId()))
		));
	}
}
