package com.lionlink.backend.domain.notification.dto.response;

import com.lionlink.backend.domain.notification.entity.NotificationTab;
import com.lionlink.backend.domain.notification.repository.TabCount;
import java.util.List;

/** 탭별 안 읽은 알림 수. all에는 탭이 없는 계정 알림도 들어간다. */
public record UnreadCountResponse(
		long all,
		long coffeeChat,
		long chat,
		long community
) {

	public static UnreadCountResponse from(List<TabCount> counts) {
		return new UnreadCountResponse(
				counts.stream().mapToLong(TabCount::count).sum(),
				count(counts, NotificationTab.COFFEE_CHAT),
				count(counts, NotificationTab.CHAT),
				count(counts, NotificationTab.COMMUNITY)
		);
	}

	private static long count(List<TabCount> counts, NotificationTab tab) {
		return counts.stream()
				.filter(count -> count.tab() == tab)
				.mapToLong(TabCount::count)
				.sum();
	}
}
