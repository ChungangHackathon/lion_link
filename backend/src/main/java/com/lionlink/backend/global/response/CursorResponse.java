package com.lionlink.backend.global.response;

import java.util.List;
import java.util.function.Function;

public record CursorResponse<T>(List<T> items, Long nextCursor, boolean hasNext) {

	public static <T> CursorResponse<T> of(List<T> fetched, int size, Function<T, Long> cursorExtractor) {
		boolean hasNext = fetched.size() > size;
		List<T> items = hasNext ? fetched.subList(0, size) : fetched;
		Long nextCursor = hasNext ? cursorExtractor.apply(items.get(items.size() - 1)) : null;
		return new CursorResponse<>(items, nextCursor, hasNext);
	}
}
