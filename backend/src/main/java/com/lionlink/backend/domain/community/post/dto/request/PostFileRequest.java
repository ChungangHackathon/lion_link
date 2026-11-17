package com.lionlink.backend.domain.community.post.dto.request;

import com.lionlink.backend.domain.community.post.entity.PostFileType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PostFileRequest(
		@NotNull(message = "파일 종류를 선택해 주세요.")
		PostFileType type,

		@NotBlank(message = "파일 주소를 입력해 주세요.")
		@Size(max = 500, message = "파일 주소는 500자 이하여야 합니다.")
		String url,

		@NotBlank(message = "파일 이름을 입력해 주세요.")
		@Size(max = 255, message = "파일 이름은 255자 이하여야 합니다.")
		String name,

		@NotNull(message = "파일 크기를 입력해 주세요.")
		@Positive(message = "빈 파일은 올릴 수 없어요.")
		Integer size
) {
}
