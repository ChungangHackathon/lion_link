package com.lionlink.backend.domain.auth.dto.response;

import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.entity.UserStatus;

public record MeResponse(
		Long id,
		String name,
		Integer generation,
		String email,
		String role,
		String status,
		boolean profileCompleted,
		String rejectReason,
		boolean signupInfoEditable,
		String photoUrl
) {

	public static MeResponse of(User user, String photoUrl) {
		return new MeResponse(
				user.getId(),
				user.getName(),
				user.getGeneration(),
				user.getEmail(),
				user.getRole().name(),
				user.getStatus().name(),
				user.isProfileCompleted(),
				user.getStatus() == UserStatus.REJECTED ? user.getRejectReason() : null,
				user.getStatus() == UserStatus.PENDING,
				photoUrl
		);
	}
}
