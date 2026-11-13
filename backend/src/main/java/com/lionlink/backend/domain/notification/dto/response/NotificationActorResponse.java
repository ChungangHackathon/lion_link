package com.lionlink.backend.domain.notification.dto.response;

import com.lionlink.backend.domain.profile.UserSummary;

/** 알림을 일으킨 회원. 탈퇴한 회원이면 name, photoUrl이 null이고 isDeleted가 true */
public record NotificationActorResponse(
		Long id,
		String name,
		String photoUrl,
		boolean isDeleted
) {

	public static NotificationActorResponse from(UserSummary summary) {
		return new NotificationActorResponse(summary.id(), summary.name(), summary.photoUrl(), summary.deleted());
	}
}
