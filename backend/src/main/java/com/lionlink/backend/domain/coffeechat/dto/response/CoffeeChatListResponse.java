package com.lionlink.backend.domain.coffeechat.dto.response;

import java.util.List;

public record CoffeeChatListResponse(
		List<CoffeeChatSummaryResponse> items,
		int page,
		int size,
		boolean hasNext,
		long pendingReceivedCount,
		long activeCount
) {
}
