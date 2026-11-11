package com.lionlink.backend.global.response;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Slice;

public record SliceResponse<T>(
		List<T> items,
		int page,
		int size,
		boolean hasNext
) {

	public static <T> SliceResponse<T> from(Slice<T> slice) {
		return new SliceResponse<>(
				slice.getContent(),
				slice.getNumber(),
				slice.getSize(),
				slice.hasNext()
		);
	}

	public static <E, T> SliceResponse<T> from(Slice<E> slice, Function<E, T> mapper) {
		return from(slice.map(mapper));
	}
}
