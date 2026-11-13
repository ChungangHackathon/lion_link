package com.lionlink.backend.domain.notification.dto.request;

import com.lionlink.backend.domain.notification.entity.NotificationTab;

/** 알림 목록 탭 필터. 드롭다운(H-07)의 전체, 커피챗, 채팅, 커뮤니티 */
public enum NotificationTabFilter {
	ALL(null),
	COFFEE_CHAT(NotificationTab.COFFEE_CHAT),
	CHAT(NotificationTab.CHAT),
	COMMUNITY(NotificationTab.COMMUNITY);

	private final NotificationTab tab;

	NotificationTabFilter(NotificationTab tab) {
		this.tab = tab;
	}

	/** ALL이면 null */
	public NotificationTab tab() {
		return tab;
	}
}
