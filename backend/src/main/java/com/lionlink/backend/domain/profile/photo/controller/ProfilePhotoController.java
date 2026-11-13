package com.lionlink.backend.domain.profile.photo.controller;

import com.lionlink.backend.domain.profile.photo.dto.response.PhotoResponse;
import com.lionlink.backend.domain.profile.photo.service.ProfilePhotoService;
import com.lionlink.backend.global.error.ErrorResponse;
import com.lionlink.backend.global.security.AllowPendingMember;
import com.lionlink.backend.global.security.CurrentUser;
import com.lionlink.backend.global.security.RequireCompletedProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** PROFILE-05 프로필 사진. 승인 대기 회원도 쓸 수 있고 반려 회원은 403 NOT_APPROVED. */
@Tag(name = "프로필")
@RestController
@RequestMapping("/me/profile/photo")
@RequiredArgsConstructor
@AllowPendingMember
@RequireCompletedProfile
@ApiResponse(responseCode = "401", description = "토큰 없음·만료 (UNAUTHENTICATED)",
		content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
@ApiResponse(responseCode = "403", description = "반려된 회원 (NOT_APPROVED), 온보딩 전 (PROFILE_NOT_COMPLETED)",
		content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class ProfilePhotoController {

	private final ProfilePhotoService profilePhotoService;

	@Operation(summary = "프로필 사진 변경 (PROFILE-05)",
			description = "multipart/form-data의 file 한 개. jpg·png·webp, 5MB 이하. 새 사진이 이전 사진을 대체하고 이전 파일은 S3에서 지운다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "400", description = "5MB 초과 (FILE_TOO_LARGE), jpg·png·webp가 아님 (UNSUPPORTED_FILE_TYPE), file 누락·빈 파일 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "502", description = "저장소에 올리지 못함 (PHOTO_UPLOAD_FAILED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PutMapping
	public PhotoResponse update(
			@Parameter(hidden = true) @CurrentUser Long userId,
			@RequestPart("file") MultipartFile file) {
		return profilePhotoService.update(userId, file);
	}

	@Operation(summary = "프로필 사진 삭제 (PROFILE-05)", description = "기본 사자 이미지로 돌아가 photoUrl이 null이 된다. 사진이 없어도 204.")
	@ApiResponse(responseCode = "204")
	@DeleteMapping
	public ResponseEntity<Void> delete(@Parameter(hidden = true) @CurrentUser Long userId) {
		profilePhotoService.delete(userId);
		return ResponseEntity.noContent().build();
	}
}
