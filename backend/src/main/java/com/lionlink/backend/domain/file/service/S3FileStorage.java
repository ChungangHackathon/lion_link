package com.lionlink.backend.domain.file.service;

import com.lionlink.backend.domain.file.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@RequiredArgsConstructor
public class S3FileStorage implements FileStorage {

	private final S3Client s3Client;
	private final StorageProperties properties;

	@Override
	public void put(String key, byte[] content, String contentType) {
		s3Client.putObject(PutObjectRequest.builder()
						.bucket(properties.bucket())
						.key(key)
						.contentType(contentType)
						.contentLength((long) content.length)
						.build(),
				RequestBody.fromBytes(content));
	}

	@Override
	public void delete(String key) {
		s3Client.deleteObject(DeleteObjectRequest.builder()
				.bucket(properties.bucket())
				.key(key)
				.build());
	}
}
