package com.lionlink.backend.domain.file.service;

import com.lionlink.backend.domain.file.config.StorageProperties;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileCleanupService {

	private final FileStorage fileStorage;
	private final StorageProperties storageProperties;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onDeleteRequested(FileDeleteRequestedEvent event) {
		event.urls().stream()
				.map(this::keyOf)
				.filter(Objects::nonNull)
				.distinct()
				.forEach(this::delete);
	}

	String keyOf(String url) {
		String prefix = storageProperties.publicBaseUrl() + "/";
		if (url == null || !url.startsWith(prefix + FileUploadService.KEY_PREFIX)) {
			return null;
		}
		String key = url.substring(prefix.length());
		return key.contains("..") ? null : key;
	}

	private void delete(String key) {
		try {
			fileStorage.delete(key);
		} catch (RuntimeException e) {
			log.warn("File delete failed. key={}, reason={}", key, e.getMessage());
		}
	}
}
