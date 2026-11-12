package com.lionlink.backend.domain.community.post.dto.response;

import com.lionlink.backend.domain.community.meetup.entity.MeetupStatus;
import com.lionlink.backend.domain.community.post.entity.PostCategory;
import com.lionlink.backend.domain.community.post.entity.PostFile;
import com.lionlink.backend.domain.community.post.entity.PostFileType;
import com.lionlink.backend.domain.community.recruit.entity.RecruitStatus;
import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.ProfileStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResponse(
		Long id,
		PostCategory category,
		String title,
		String content,
		boolean isAnonymous,
		boolean isMine,
		AuthorDetail author,
		List<String> links,
		List<FileResponse> files,
		LocalDate deadline,
		int viewCount,
		long commentCount,
		long likeCount,
		boolean likedByMe,
		MeetupDetail meetup,
		RecruitDetail recruit,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {

	public record AuthorDetail(
			Long id,
			String name,
			Integer generation,
			List<LionPart> lionParts,
			String photoUrl,
			String headline,
			ProfileStatus status,
			boolean isDeleted
	) {
	}

	public record FileResponse(Long id, PostFileType type, String url, String name, Integer size) {

		public static FileResponse from(PostFile file) {
			return new FileResponse(file.getId(), file.getType(), file.getUrl(), file.getName(), file.getSize());
		}
	}

	public record MeetupDetail(
			LocalDateTime meetAt,
			String place,
			Integer capacity,
			long participantCount,
			MeetupStatus status,
			boolean joinedByMe,
			List<PostMemberResponse> members
	) {
	}

	public record RecruitDetail(
			SeniorDetail senior,
			Integer capacity,
			long memberCount,
			LocalDate deadline,
			RecruitStatus status,
			boolean joinedByMe,
			boolean isSenior,
			boolean hasOpenCoffeeChatWithSenior,
			List<PostMemberResponse> members
	) {
	}

	public record SeniorDetail(
			Long id,
			String name,
			Integer generation,
			List<LionPart> lionParts,
			String photoUrl,
			boolean acceptGroup,
			boolean isDeleted
	) {
	}
}
