package com.lionlink.backend.domain.file.service;

import com.lionlink.backend.domain.file.FileType;
import com.lionlink.backend.domain.file.config.StorageProperties;
import com.lionlink.backend.domain.file.dto.request.FileUploadRequest;
import com.lionlink.backend.domain.file.dto.response.FileUploadResponse;
import com.lionlink.backend.domain.file.exception.FileErrorCode;
import com.lionlink.backend.global.config.JacksonConfig;
import com.lionlink.backend.global.error.BusinessException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class FileUploadService {

	private static final String KEY_PREFIX = "uploads/";

	private final S3Presigner s3Presigner;
	private final StorageProperties storageProperties;

	public FileUploadResponse issueUploadUrl(Long userId, FileUploadRequest request) {
		FileType type = request.type();
		String extension = extension(request.fileName());
		String contentType = type.contentTypeOf(extension)
				.orElseThrow(() -> new BusinessException(FileErrorCode.INVALID_FILE_TYPE));
		if (request.size() > type.maxSize()) {
			throw new BusinessException(FileErrorCode.FILE_TOO_LARGE);
		}

		String key = KEY_PREFIX + userId + "/" + UUID.randomUUID() + "." + extension;
		PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(PutObjectPresignRequest.builder()
				.signatureDuration(storageProperties.uploadUrlTtl())
				.putObjectRequest(PutObjectRequest.builder()
						.bucket(storageProperties.bucket())
						.key(key)
						.contentType(contentType)
						.contentLength(request.size())
						.build())
				.build());

		return new FileUploadResponse(
				presigned.url().toString(),
				storageProperties.publicBaseUrl() + "/" + key,
				contentType,
				LocalDateTime.ofInstant(presigned.expiration().truncatedTo(ChronoUnit.SECONDS), JacksonConfig.ZONE)
		);
	}

	public boolean isUploadedBy(Long userId, String fileUrl) {
		String prefix = storageProperties.publicBaseUrl() + "/" + KEY_PREFIX + userId + "/";
		return fileUrl != null && fileUrl.startsWith(prefix) && !fileUrl.substring(prefix.length()).contains("/");
	}

	private String extension(String fileName) {
		int dot = fileName.lastIndexOf('.');
		if (dot < 0 || dot == fileName.length() - 1) {
			throw new BusinessException(FileErrorCode.INVALID_FILE_TYPE);
		}
		return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
	}
}
