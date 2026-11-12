package com.lionlink.backend.domain.community.meetup.dto.response;

import com.lionlink.backend.domain.community.meetup.entity.MeetupStatus;
import com.lionlink.backend.domain.community.post.dto.response.PostMemberResponse;
import java.util.List;

public record MeetupParticipationResponse(
		Long postId,
		boolean joined,
		long participantCount,
		Integer capacity,
		MeetupStatus status,
		List<PostMemberResponse> members
) {
}
