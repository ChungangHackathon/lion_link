package com.lionlink.backend.domain.file.dto.response;

import java.time.LocalDateTime;

public record FileUploadResponse(
		String uploadUrl,
		String fileUrl,
		String contentType,
		LocalDateTime expiresAt
) {
}
