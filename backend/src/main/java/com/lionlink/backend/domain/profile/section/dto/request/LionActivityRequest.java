package com.lionlink.backend.domain.profile.section.dto.request;

import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.section.lionactivity.LionRole;
import com.lionlink.backend.domain.profile.validation.LionPartSelection;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** PROFILE-11 멋사 활동 추가·수정. 가입 기수 항목(isDefault)은 generation을 못 바꾼다. */
public record LionActivityRequest(
		@NotNull(message = "기수를 입력해 주세요.")
		@Min(value = 1, message = "기수는 1~14 사이로 입력해 주세요.")
		@Max(value = 14, message = "기수는 1~14 사이로 입력해 주세요.")
		Integer generation,

		@NotNull(message = "역할을 선택해 주세요.")
		LionRole role,

		@LionPartSelection
		List<@NotNull(message = "멋사 파트를 선택해 주세요.") LionPart> parts
) {
}
