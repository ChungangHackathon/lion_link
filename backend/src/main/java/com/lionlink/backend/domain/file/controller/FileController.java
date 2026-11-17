package com.lionlink.backend.domain.file.controller;

import com.lionlink.backend.domain.file.dto.request.FileUploadRequest;
import com.lionlink.backend.domain.file.dto.response.FileUploadResponse;
import com.lionlink.backend.domain.file.service.FileUploadService;
import com.lionlink.backend.global.error.ErrorResponse;
import com.lionlink.backend.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "파일")
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

	private final FileUploadService fileUploadService;

	@Operation(summary = "파일 업로드 주소 발급",
			description = "uploadUrl로 파일을 PUT하고(Content-Type은 응답의 contentType), 글을 쓸 때 fileUrl을 보낸다. 주소는 10분 동안 쓸 수 있다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "400", description = "지원하지 않는 형식이거나 너무 큰 파일 (VALIDATION_ERROR, INVALID_FILE_TYPE, FILE_TOO_LARGE)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping
	public FileUploadResponse issueUploadUrl(@CurrentUser Long userId, @Valid @RequestBody FileUploadRequest request) {
		return fileUploadService.issueUploadUrl(userId, request);
	}
}
