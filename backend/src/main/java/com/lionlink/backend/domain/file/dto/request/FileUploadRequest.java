package com.lionlink.backend.domain.file.dto.request;

import com.lionlink.backend.domain.file.FileType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record FileUploadRequest(
		@NotNull(message = "파일 종류를 선택해 주세요.")
		FileType type,

		@NotBlank(message = "파일 이름을 입력해 주세요.")
		@Size(max = 255, message = "파일 이름은 255자 이하여야 합니다.")
		String fileName,

		@NotNull(message = "파일 크기를 입력해 주세요.")
		@Positive(message = "빈 파일은 올릴 수 없어요.")
		Long size
) {
}
