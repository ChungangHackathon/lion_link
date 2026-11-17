package com.lionlink.backend.domain.file;

import java.util.Map;
import java.util.Optional;

public enum FileType {

	IMAGE(10L * 1024 * 1024, Map.of(
			"jpg", "image/jpeg",
			"jpeg", "image/jpeg",
			"png", "image/png",
			"webp", "image/webp"
	)),
	ATTACHMENT(20L * 1024 * 1024, Map.of(
			"pdf", "application/pdf",
			"hwp", "application/x-hwp",
			"docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			"pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
			"xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
			"zip", "application/zip"
	));

	private final long maxSize;
	private final Map<String, String> contentTypes;

	FileType(long maxSize, Map<String, String> contentTypes) {
		this.maxSize = maxSize;
		this.contentTypes = contentTypes;
	}

	public long maxSize() {
		return maxSize;
	}

	public Optional<String> contentTypeOf(String extension) {
		return Optional.ofNullable(contentTypes.get(extension));
	}
}
