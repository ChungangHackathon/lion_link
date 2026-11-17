package com.lionlink.backend.domain.community.recruit.dto.response;

import com.lionlink.backend.domain.community.post.dto.response.PostMemberResponse;
import com.lionlink.backend.domain.community.recruit.entity.RecruitStatus;
import java.util.List;

public record RecruitParticipationResponse(
		Long postId,
		boolean joined,
		long memberCount,
		Integer capacity,
		RecruitStatus status,
		List<PostMemberResponse> members
) {
}
