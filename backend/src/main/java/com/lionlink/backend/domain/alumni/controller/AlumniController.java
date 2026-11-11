package com.lionlink.backend.domain.alumni.controller;

import com.lionlink.backend.domain.alumni.dto.request.AlumniSearchCondition;
import com.lionlink.backend.domain.alumni.dto.response.AlumniDetailResponse;
import com.lionlink.backend.domain.alumni.dto.response.AlumniResponse;
import com.lionlink.backend.domain.alumni.service.AlumniService;
import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.ProfileStatus;
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
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "동문 찾기")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class AlumniController {

	private final AlumniService alumniService;

	@Operation(summary = "동문 목록 조회 및 검색",
			description = "같은 필터 안의 값은 OR, 서로 다른 필터는 AND로 묶는다. 최근 가입한 순서로 반환한다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "400", description = "잘못된 검색 조건 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "403", description = "가입 승인 전 회원 (NOT_APPROVED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping
	public PageResponse<AlumniResponse> search(
			@CurrentUser Long userId,
			@Parameter(description = "이름·회사·직무·기술 검색어")
			@RequestParam(required = false)
			@Size(max = 50, message = "검색어는 50자 이하여야 합니다.") String q,
			@Parameter(description = "기수 (예: 13,14)") @RequestParam(required = false) List<Integer> generations,
			@Parameter(description = "파트") @RequestParam(required = false) List<LionPart> parts,
			@Parameter(description = "현재 상태") @RequestParam(required = false) List<ProfileStatus> statuses,
			@Parameter(description = "관심 분야 (예: BACKEND,DATA_AI)") @RequestParam(required = false) List<String> interests,
			@Parameter(description = "페이지 번호 (0부터)")
			@RequestParam(defaultValue = "0")
			@Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.") int page,
			@Parameter(description = "페이지 크기 (1~100)")
			@RequestParam(defaultValue = "20")
			@Min(value = 1, message = "페이지 크기는 1~100이어야 합니다.")
			@Max(value = 100, message = "페이지 크기는 1~100이어야 합니다.") int size
	) {
		AlumniSearchCondition condition = new AlumniSearchCondition(q, generations, parts, statuses, interests);
		return alumniService.search(userId, condition, PageRequest.of(page, size));
	}

	@Operation(summary = "동문 프로필 상세 조회",
			description = "공개 범위에 따라 이메일·전화번호·링크·회사 정보는 null로 내려간다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "404", description = "동문 없음 또는 탈퇴 (USER_NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/{userId}")
	public AlumniDetailResponse getDetail(
			@CurrentUser Long currentUserId,
			@Parameter(description = "조회할 동문의 회원 ID") @PathVariable Long userId
	) {
		return alumniService.getDetail(currentUserId, userId);
	}
}
