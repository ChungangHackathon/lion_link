package com.lionlink.backend.domain.admin.dto.response;

import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.ProfileStatus;
import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "가입 목록 한 건")
public record SignupListItemResponse(
		Long userId,
		@Schema(description = "가입할 때 본인이 입력한 이름") String name,
		@Schema(description = "가입할 때 본인이 입력한 기수") Integer generation,
		String email,
		@Schema(description = "구글 계정 이름. 입력 이름과 대조하는 용도") String googleName,
		LocalDateTime signedUpAt,
		@Schema(description = "온보딩 전이면 빈 배열") List<LionPart> lionParts,
		@Schema(description = "온보딩 전이면 null") ProfileStatus currentStatus,
		@Schema(description = "현재 재직 중 경력의 회사명. 비공개 설정과 상관없이 보인다. 없으면 null") String company,
		@Schema(description = "이미 승인된 다른 회원 중 이름·기수가 같은 사람이 있는지") boolean sameNameGeneration,
		@Schema(description = "반려된 뒤 다시 신청했는지") boolean reapplied,
		LocalDateTime reapplyAt,
		@Schema(description = "이전 반려 사유. 없으면 null") String rejectReason,
		@Schema(description = "승인 대기면 null. 승인 절차 없이 승인된 계정(관리자 등)도 null") LocalDateTime reviewedAt,
		@Schema(description = "처리한 관리자 이름. 승인 대기면 null") String reviewedBy
) {

	public static SignupListItemResponse of(User user, List<LionPart> lionParts, ProfileStatus currentStatus,
			String company, boolean sameNameGeneration, String reviewerName) {
		// 재신청한 회원은 승인 대기인데도 이전 반려의 처리 정보가 남아 있어, 대기 상태에서는 숨긴다.
		boolean reviewed = user.getStatus() != UserStatus.PENDING;
		return new SignupListItemResponse(
				user.getId(),
				user.getName(),
				user.getGeneration(),
				user.getEmail(),
				user.getGoogleName(),
				user.getCreatedAt(),
				lionParts,
				currentStatus,
				company,
				sameNameGeneration,
				user.getReappliedAt() != null,
				user.getReappliedAt(),
				user.getRejectReason(),
				reviewed ? user.getReviewedAt() : null,
				reviewed ? reviewerName : null
		);
	}
}
