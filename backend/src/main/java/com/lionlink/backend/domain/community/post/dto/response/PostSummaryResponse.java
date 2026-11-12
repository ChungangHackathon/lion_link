package com.lionlink.backend.domain.community.post.dto.response;

import com.lionlink.backend.domain.community.meetup.entity.MeetupStatus;
import com.lionlink.backend.domain.community.post.entity.PostCategory;
import com.lionlink.backend.domain.community.recruit.entity.RecruitStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PostSummaryResponse(
		Long id,
		PostCategory category,
		String title,
		boolean isAnonymous,
		PostAuthorResponse author,
		int viewCount,
		long commentCount,
		boolean hasFile,
		String thumbnailUrl,
		LocalDate deadline,
		MeetupSummary meetup,
		RecruitSummary recruit,
		LocalDateTime createdAt
) {

	public record MeetupSummary(
			LocalDateTime meetAt,
			String place,
			Integer capacity,
			long participantCount,
			MeetupStatus status
	) {
	}

	public record RecruitSummary(
			SeniorSummary senior,
			Integer capacity,
			long memberCount,
			LocalDate deadline,
			RecruitStatus status
	) {
	}

	public record SeniorSummary(Long id, String name, Integer generation, boolean isDeleted) {
	}
}
