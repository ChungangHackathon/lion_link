package com.lionlink.backend.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.lionlink.backend.domain.file.FileType;
import com.lionlink.backend.domain.file.dto.request.FileUploadRequest;
import com.lionlink.backend.domain.file.dto.response.FileUploadResponse;
import com.lionlink.backend.domain.file.exception.FileErrorCode;
import com.lionlink.backend.global.error.BusinessException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FileUploadServiceTest {

	private static final String BASE_URL = "https://test-bucket.s3.ap-northeast-2.amazonaws.com";
	private static final long MB = 1024 * 1024;

	@Autowired
	private FileUploadService fileUploadService;

	@Test
	void 업로드_주소와_올린_뒤_보여줄_주소를_발급한다() {
		FileUploadResponse response = fileUploadService.issueUploadUrl(7L, new FileUploadRequest(FileType.IMAGE, "사진.PNG", 3 * MB));

		assertThat(response.fileUrl()).startsWith(BASE_URL + "/uploads/7/").endsWith(".png");
		assertThat(response.uploadUrl()).startsWith(response.fileUrl() + "?")
				.contains("X-Amz-Signature=")
				.contains("X-Amz-SignedHeaders=content-length%3Bcontent-type%3Bhost");
		assertThat(response.contentType()).isEqualTo("image/png");
		assertThat(response.expiresAt()).isCloseTo(LocalDateTime.now().plusMinutes(10), within(5, ChronoUnit.SECONDS));
	}

	@Test
	void 같은_파일_이름이어도_주소가_겹치지_않는다() {
		FileUploadRequest request = new FileUploadRequest(FileType.ATTACHMENT, "기출문제.pdf", MB);

		assertThat(fileUploadService.issueUploadUrl(7L, request).fileUrl())
				.isNotEqualTo(fileUploadService.issueUploadUrl(7L, request).fileUrl());
	}

	@Test
	void 종류에_맞지_않는_확장자는_거절한다() {
		assertError(new FileUploadRequest(FileType.IMAGE, "자료.pdf", MB), FileErrorCode.INVALID_FILE_TYPE);
		assertError(new FileUploadRequest(FileType.ATTACHMENT, "사진.png", MB), FileErrorCode.INVALID_FILE_TYPE);
		assertError(new FileUploadRequest(FileType.ATTACHMENT, "확장자없음", MB), FileErrorCode.INVALID_FILE_TYPE);
		assertError(new FileUploadRequest(FileType.ATTACHMENT, "점으로끝남.", MB), FileErrorCode.INVALID_FILE_TYPE);
	}

	@Test
	void 이미지는_10MB_첨부파일은_20MB까지_올릴_수_있다() {
		assertThat(fileUploadService.issueUploadUrl(7L, new FileUploadRequest(FileType.IMAGE, "a.jpg", 10 * MB)).contentType())
				.isEqualTo("image/jpeg");
		assertError(new FileUploadRequest(FileType.IMAGE, "a.jpg", 10 * MB + 1), FileErrorCode.FILE_TOO_LARGE);
		assertThat(fileUploadService.issueUploadUrl(7L, new FileUploadRequest(FileType.ATTACHMENT, "a.hwp", 20 * MB)).contentType())
				.isEqualTo("application/x-hwp");
		assertError(new FileUploadRequest(FileType.ATTACHMENT, "a.zip", 20 * MB + 1), FileErrorCode.FILE_TOO_LARGE);
	}

	@Test
	void 내가_올린_파일_주소인지_확인한다() {
		String mine = fileUploadService.issueUploadUrl(7L, new FileUploadRequest(FileType.IMAGE, "a.webp", MB)).fileUrl();

		assertThat(fileUploadService.isUploadedBy(7L, mine)).isTrue();
		assertThat(fileUploadService.isUploadedBy(8L, mine)).isFalse();
		assertThat(fileUploadService.isUploadedBy(7L, "https://other.com/uploads/7/a.png")).isFalse();
		assertThat(fileUploadService.isUploadedBy(7L, BASE_URL + "/uploads/7/sub/a.png")).isFalse();
		assertThat(fileUploadService.isUploadedBy(7L, null)).isFalse();
	}

	private void assertError(FileUploadRequest request, FileErrorCode errorCode) {
		assertThatThrownBy(() -> fileUploadService.issueUploadUrl(7L, request))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(errorCode);
	}
}
