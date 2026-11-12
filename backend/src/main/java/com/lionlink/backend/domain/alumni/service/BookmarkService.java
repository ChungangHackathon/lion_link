package com.lionlink.backend.domain.alumni.service;

import com.lionlink.backend.domain.alumni.dto.response.BookmarkResponse;
import com.lionlink.backend.domain.alumni.entity.Bookmark;
import com.lionlink.backend.domain.alumni.entity.BookmarkId;
import com.lionlink.backend.domain.alumni.exception.AlumniErrorCode;
import com.lionlink.backend.domain.alumni.repository.BookmarkRepository;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookmarkService {

	private final BookmarkRepository bookmarkRepository;
	private final UserRepository userRepository;

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
}
