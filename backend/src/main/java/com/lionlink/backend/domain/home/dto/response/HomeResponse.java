package com.lionlink.backend.domain.home.dto.response;

import com.lionlink.backend.domain.alumni.dto.response.AlumniResponse;
import com.lionlink.backend.domain.coffeechat.entity.MeetingMode;
import com.lionlink.backend.domain.community.post.entity.PostCategory;
import com.lionlink.backend.domain.profile.LionPart;
import java.time.LocalDateTime;
import java.util.List;

public record HomeResponse(
		Me me,
		boolean welcomeBannerVisible,
		Counts counts,
		UpcomingCoffeeChat upcomingCoffeeChat,
		List<AlumniResponse> recommendedSeniors,
		OnThisDay onThisDay
) {

	public record Me(
			Long id,
			String name,
			Integer generation,
			List<LionPart> lionParts,
			String headline,
			String bio,
			String photoUrl
	) {
	}

	public record Counts(
			long pendingCoffeeChats,
			long activeCoffeeChats,
			long unreadMessages,
			long messageRequests,
			long bookmarks
	) {
	}

	public record UpcomingCoffeeChat(
			Long id,
			LocalDateTime scheduledAt,
			MeetingMode meetingMode,
			Long roomId,
			Partner partner
	) {
	}

	public record Partner(
			Long id,
			String name,
			Integer generation,
			List<LionPart> lionParts,
			String photoUrl,
			boolean isDeleted
	) {
	}

	public record OnThisDay(
			Long id,
			PostCategory category,
			String title,
			String thumbnailUrl,
			LocalDateTime createdAt
	) {
	}
}
