package com.lionlink.backend.domain.home.controller;

import com.lionlink.backend.domain.home.dto.response.GuideDismissResponse;
import com.lionlink.backend.domain.home.dto.response.HomeResponse;
import com.lionlink.backend.domain.home.service.HomeService;
import com.lionlink.backend.global.error.ErrorResponse;
import com.lionlink.backend.global.security.CurrentUser;
import com.lionlink.backend.global.security.RequireCompletedProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "홈")
@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
public class HomeController {

	private final HomeService homeService;

	@Operation(summary = "홈 화면 조회",
			description = "커뮤니티 새 소식과 모집 중인 글은 게시글 목록 API(category, recruiting)로 조회한다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "403", description = "가입 승인 전 또는 기본 프로필 미완료 (NOT_APPROVED, PROFILE_NOT_COMPLETED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@RequireCompletedProfile
	@GetMapping
	public HomeResponse getHome(@CurrentUser Long userId) {
		return homeService.getHome(userId);
	}

	@Operation(summary = "환영 배너 닫기", description = "여러 번 호출해도 처음 닫은 시각을 유지한다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "403", description = "가입 승인 전 (NOT_APPROVED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/coffee-guide/dismiss")
	public GuideDismissResponse dismissGuide(@CurrentUser Long userId) {
		return homeService.dismissGuide(userId);
	}
}
