package com.lionlink.backend.domain.auth.controller;

import com.lionlink.backend.domain.auth.dto.request.SignupRequest;
import com.lionlink.backend.domain.auth.dto.response.GoogleInfoResponse;
import com.lionlink.backend.domain.auth.dto.response.SessionResponse;
import com.lionlink.backend.domain.auth.service.AuthService;
import com.lionlink.backend.domain.auth.service.Issued;
import com.lionlink.backend.global.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 구글 로그인 시작(AUTH-01)은 컨트롤러가 아니라 Spring Security가 처리한다.
 * 프론트는 브라우저를 GET /oauth2/authorization/google 로 이동시키면 된다.
 */
@Tag(name = "계정·인증")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final AuthCookies authCookies;

	@Operation(summary = "가입 화면 구글 정보 조회",
			description = "구글 로그인 후 /signup 으로 돌아온 프론트가 이메일·이름 칸을 미리 채울 때 쓴다. signupToken 쿠키 필요.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "401", description = "signupToken 쿠키 없음·만료·위조 (UNAUTHENTICATED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/signup/google-info")
	public GoogleInfoResponse googleInfo(
			@Parameter(hidden = true) @CookieValue(name = AuthCookies.SIGNUP_TOKEN, required = false) String signupToken) {
		return authService.getSignupGoogleInfo(signupToken);
	}

	@Operation(summary = "가입 신청 (AUTH-02)",
			description = "승인 대기(PENDING) 상태로 계정을 만들고 바로 로그인시킨다. signupToken 쿠키 필요. "
					+ "성공하면 refreshToken 쿠키를 심고 signupToken 쿠키는 지운다.")
	@ApiResponse(responseCode = "201")
	@ApiResponse(responseCode = "400", description = "글자 수·범위 오류, 개인정보 미동의 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "signupToken 쿠키 없음·만료·위조 (UNAUTHENTICATED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "이미 가입된 구글 계정 (ALREADY_REGISTERED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/signup")
	public ResponseEntity<SessionResponse> signup(
			@Parameter(hidden = true) @CookieValue(name = AuthCookies.SIGNUP_TOKEN, required = false) String signupToken,
			@Valid @RequestBody SignupRequest request) {
		Issued<SessionResponse> issued = authService.signup(signupToken, request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.header(HttpHeaders.SET_COOKIE,
						authCookies.refreshToken(issued.refreshToken()),
						authCookies.expireSignupToken())
				.body(issued.body());
	}
}
