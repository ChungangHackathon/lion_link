package com.lionlink.backend.domain.profile.basic.dto.request;

import com.lionlink.backend.domain.profile.basic.entity.Visibility;
import com.lionlink.backend.domain.profile.validation.NoConnected;

/** PROFILE-10. 보낸 키만 바꾼다. 회사명은 CONNECTED를 쓸 수 없다. */
public record VisibilityUpdateRequest(
		Visibility email,
		Visibility phone,
		Visibility links,
		@NoConnected(message = "회사명은 전체 공개 또는 비공개만 고를 수 있어요.") Visibility company
) {
}
