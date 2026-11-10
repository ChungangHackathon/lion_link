package com.lionlink.backend.global.response;

import java.util.List;
import java.util.function.Function;

public record CursorResponse<T>(List<T> content, Long nextCursor, boolean hasNext) {

	/**
	 * size + 1개를 조회해서 넘기면, 다음 페이지 여부를 판단하고 size개만 담는다.
	 */
	public static <T> CursorResponse<T> of(List<T> fetched, int size, Function<T, Long> cursorExtractor) {
		boolean hasNext = fetched.size() > size;
		List<T> content = hasNext ? fetched.subList(0, size) : fetched;
		Long nextCursor = hasNext ? cursorExtractor.apply(content.get(content.size() - 1)) : null;
		return new CursorResponse<>(content, nextCursor, hasNext);
	}
}
