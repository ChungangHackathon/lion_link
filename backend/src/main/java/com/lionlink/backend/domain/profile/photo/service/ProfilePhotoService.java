package com.lionlink.backend.domain.profile.photo.service;

import com.lionlink.backend.domain.file.config.StorageProperties;
import com.lionlink.backend.domain.file.exception.FileErrorCode;
import com.lionlink.backend.domain.file.service.FileStorage;
import com.lionlink.backend.domain.profile.basic.entity.Profile;
import com.lionlink.backend.domain.profile.exception.ProfileErrorCode;
import com.lionlink.backend.domain.profile.photo.dto.response.PhotoResponse;
import com.lionlink.backend.domain.profile.section.service.SectionItemSupport;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.CommonErrorCode;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** PROFILE-05 프로필 사진 변경·삭제. 새 사진을 올리고 주소를 바꾼 뒤 이전 파일은 바로 지운다. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfilePhotoService {

	static final long MAX_SIZE = 5L * 1024 * 1024;
	private static final String KEY_PREFIX = "profile-photos/";
	private static final Map<String, String> CONTENT_TYPES = Map.of(
			"jpg", "image/jpeg",
			"jpeg", "image/jpeg",
			"png", "image/png",
			"webp", "image/webp");

	private final SectionItemSupport support;
	private final FileStorage fileStorage;
	private final StorageProperties storageProperties;

	@Transactional
	public PhotoResponse update(Long userId, MultipartFile file) {
		String extension = extension(file.getOriginalFilename());
		String contentType = CONTENT_TYPES.get(extension);
		if (file.isEmpty()) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR, "사진 파일을 골라 주세요.");
		}
		if (file.getSize() > MAX_SIZE) {
			throw new BusinessException(FileErrorCode.FILE_TOO_LARGE);
		}
		byte[] content = read(file);
		// 확장자만 바꾼 파일을 막으려고 파일 앞부분 바이트도 확인한다.
		if (!contentType.equals(detectContentType(content))) {
			throw new BusinessException(ProfileErrorCode.UNSUPPORTED_FILE_TYPE);
		}

		Profile profile = support.lockProfile(userId);
		String key = KEY_PREFIX + userId + "/" + UUID.randomUUID() + "." + extension;
		try {
			fileStorage.put(key, content, contentType);
		} catch (RuntimeException e) {
			log.error("프로필 사진 업로드 실패 (userId={})", userId, e);
			throw new BusinessException(ProfileErrorCode.PHOTO_UPLOAD_FAILED);
		}

		String oldUrl = profile.getPhotoUrl();
		try {
			profile.updatePhoto(storageProperties.publicBaseUrl() + "/" + key);
		} catch (RuntimeException e) {
			deleteQuietly(key);
			throw e;
		}
		deleteQuietly(keyOf(oldUrl));
		return new PhotoResponse(profile.getPhotoUrl());
	}

	/** 사진이 없어도 204. 기본 사자 이미지는 FE가 photoUrl이 null일 때 쓴다. */
	@Transactional
	public void delete(Long userId) {
		Profile profile = support.lockProfile(userId);
		String oldUrl = profile.getPhotoUrl();
		profile.updatePhoto(null);
		deleteQuietly(keyOf(oldUrl));
	}

	/** 우리 버킷의 주소만 키로 바꾼다. 다른 곳의 주소는 건드리지 않는다. */
	private String keyOf(String photoUrl) {
		String prefix = storageProperties.publicBaseUrl() + "/";
		if (photoUrl == null || !photoUrl.startsWith(prefix)) {
			return null;
		}
		return photoUrl.substring(prefix.length());
	}

	/** 이전 파일 삭제는 실패해도 사진 변경 자체는 성공으로 둔다(파일만 남는다). */
	private void deleteQuietly(String key) {
		if (key == null) {
			return;
		}
		try {
			fileStorage.delete(key);
		} catch (RuntimeException e) {
			log.warn("프로필 사진 파일 삭제 실패 (key={})", key, e);
		}
	}

	private static String extension(String fileName) {
		int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
		String extension = dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
		if (!CONTENT_TYPES.containsKey(extension)) {
			throw new BusinessException(ProfileErrorCode.UNSUPPORTED_FILE_TYPE);
		}
		return extension;
	}

	private static byte[] read(MultipartFile file) {
		try {
			return file.getBytes();
		} catch (IOException e) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR, "사진 파일을 읽지 못했어요.");
		}
	}

	/** jpg(FF D8 FF), png(89 'PNG'), webp('RIFF'....'WEBP') 중 무엇인지. 아니면 null. */
	private static String detectContentType(byte[] b) {
		if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
			return "image/jpeg";
		}
		if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
				&& b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
			return "image/png";
		}
		if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
				&& b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
			return "image/webp";
		}
		return null;
	}
}
