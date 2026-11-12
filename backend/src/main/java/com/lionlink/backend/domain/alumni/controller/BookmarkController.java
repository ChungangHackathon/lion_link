package com.lionlink.backend.domain.alumni.controller;

import com.lionlink.backend.domain.alumni.dto.response.BookmarkResponse;
import com.lionlink.backend.domain.alumni.dto.response.BookmarkedAlumniResponse;
import com.lionlink.backend.domain.alumni.service.AlumniService;
import com.lionlink.backend.domain.alumni.service.BookmarkService;
import com.lionlink.backend.global.error.ErrorResponse;
import com.lionlink.backend.global.response.PageResponse;
import com.lionlink.backend.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관심 동문")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class BookmarkController {

	private final BookmarkService bookmarkService;
	private final AlumniService alumniService;

	@Operation(summary = "관심 동문 목록 조회", description = "최근 저장한 순서로 반환하고, 탈퇴한 동문은 뺀다. 항목은 동문 목록과 같은 형식에 bookmarkedAt이 붙는다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "400", description = "잘못된 조회 조건 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "미인증 (UNAUTHENTICATED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/me/bookmarks")
	public PageResponse<BookmarkedAlumniResponse> getBookmarks(
			@CurrentUser Long userId,
			@Parameter(description = "페이지 번호 (0부터)")
			@RequestParam(defaultValue = "0")
			@Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.") int page,
			@Parameter(description = "페이지 크기 (1~100)")
			@RequestParam(defaultValue = "20")
			@Min(value = 1, message = "페이지 크기는 1~100이어야 합니다.")
			@Max(value = 100, message = "페이지 크기는 1~100이어야 합니다.") int size
	) {
		return alumniService.getBookmarks(userId, PageRequest.of(page, size));
	}

	@Operation(summary = "관심 동문 저장", description = "이미 저장한 동문이면 그대로 성공한다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "400", description = "자기 자신 저장 (CANNOT_BOOKMARK_SELF)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "대상 회원 없음 (USER_NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PutMapping("/{userId}/bookmark")
	public BookmarkResponse bookmark(
			@CurrentUser Long currentUserId,
			@Parameter(description = "저장할 동문의 회원 ID") @PathVariable Long userId
	) {
		return bookmarkService.bookmark(currentUserId, userId);
	}

	@Operation(summary = "관심 동문 저장 취소", description = "저장하지 않은 동문이어도 204를 반환한다.")
	@ApiResponse(responseCode = "204")
	@ApiResponse(responseCode = "401", description = "미인증 (UNAUTHENTICATED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@DeleteMapping("/{userId}/bookmark")
	public ResponseEntity<Void> unbookmark(
			@CurrentUser Long currentUserId,
			@Parameter(description = "저장을 취소할 동문의 회원 ID") @PathVariable Long userId
	) {
		bookmarkService.unbookmark(currentUserId, userId);
		return ResponseEntity.noContent().build();
	}
}
