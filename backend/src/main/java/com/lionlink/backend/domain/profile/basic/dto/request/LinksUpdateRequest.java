package com.lionlink.backend.domain.profile.basic.dto.request;

import com.lionlink.backend.domain.profile.basic.entity.LinkType;
import com.lionlink.backend.domain.profile.validation.HttpUrl;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** PROFILE-08. 보낸 순서 그대로 저장한다. null이나 빈 배열은 섹션 비우기. */
public record LinksUpdateRequest(
		@Size(max = 5, message = "외부 링크는 최대 5개까지 입력할 수 있어요.")
		List<@Valid @NotNull(message = "링크를 입력해 주세요.") LinkInput> links
) {

	public record LinkInput(
			@NotNull(message = "링크 종류를 선택해 주세요.") LinkType type,
			@NotNull(message = "링크 주소를 입력해 주세요.") @HttpUrl String url
	) {
	}
}
