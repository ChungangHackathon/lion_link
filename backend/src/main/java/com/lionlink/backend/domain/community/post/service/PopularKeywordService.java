package com.lionlink.backend.domain.community.post.service;

import com.lionlink.backend.domain.community.post.dto.response.PopularKeywordResponse;
import com.lionlink.backend.domain.community.post.repository.SearchKeywordRepository;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PopularKeywordService {

	public static final int WINDOW_DAYS = 7;
	private static final int MIN_LENGTH = 2;
	private static final int MAX_LENGTH = 20;

	private final SearchKeywordRepository searchKeywordRepository;

	@Transactional
	public void record(Long userId, String q) {
		String keyword = normalize(q);
		if (keyword != null) {
			searchKeywordRepository.upsert(userId, keyword, LocalDateTime.now());
		}
	}

	public PopularKeywordResponse getPopular(int size) {
		LocalDateTime from = LocalDateTime.now().minusDays(WINDOW_DAYS);
		return new PopularKeywordResponse(searchKeywordRepository.findPopular(from, PageRequest.of(0, size)).stream()
				.map(row -> new PopularKeywordResponse.Item(row.getKeyword(), row.getTotal()))
				.toList());
	}

	@Transactional
	public int deleteExpired(LocalDateTime now) {
		return searchKeywordRepository.deleteSearchedBefore(now.minusDays(WINDOW_DAYS));
	}

	static String normalize(String q) {
		if (q == null) {
			return null;
		}
		String keyword = q.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
		int length = keyword.codePointCount(0, keyword.length());
		return length < MIN_LENGTH || length > MAX_LENGTH ? null : keyword;
	}
}
