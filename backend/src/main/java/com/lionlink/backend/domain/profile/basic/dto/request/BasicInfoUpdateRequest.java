package com.lionlink.backend.domain.profile.basic.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lionlink.backend.domain.profile.Interest;
import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.ProfileStatus;
import com.lionlink.backend.domain.profile.validation.InterestSelection;
import com.lionlink.backend.domain.profile.validation.LionPartSelection;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * PROFILE-04. 이름·기수는 받지 않는다. 보낸 값으로 통째로 바꾸므로 안 바꾼 칸도 현재 값을 보내야 한다.
 * company·jobTitle은 재직·창업 상태일 때 필수이고, 그 외 상태에서는 보내도 무시하고 null로 저장한다.
 */
public record BasicInfoUpdateRequest(
		@Size(max = 100, message = "한 줄 소개는 100자 이하로 입력해 주세요.") String headline,
		@Size(max = 30, message = "주전공은 30자 이하로 입력해 주세요.") String major,
		@Size(max = 30, message = "복수·부전공은 30자 이하로 입력해 주세요.") String minor,
		@Size(max = 20, message = "활동 지역은 20자 이하로 입력해 주세요.") String region,
		@NotNull(message = "현재 상태를 선택해 주세요.") ProfileStatus status,
		@LionPartSelection List<@NotNull(message = "멋사 파트를 선택해 주세요.") LionPart> lionParts,
		@InterestSelection List<@NotNull(message = "관심 분야를 선택해 주세요.") Interest> interests,
		@Size(min = 1, max = 30, message = "회사명은 1~30자로 입력해 주세요.") String company,
		@Size(min = 1, max = 30, message = "직무는 1~30자로 입력해 주세요.") String jobTitle
) {

	@JsonIgnore
	@AssertTrue(message = "재직·창업 중이면 회사명과 직무를 입력해 주세요.")
	public boolean isWorkingInfoPresent() {
		return status == null || !status.isWorking()
				|| (company != null && !company.isBlank() && jobTitle != null && !jobTitle.isBlank());
	}
}
