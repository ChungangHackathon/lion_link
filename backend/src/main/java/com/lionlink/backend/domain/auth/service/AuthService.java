package com.lionlink.backend.domain.auth.service;

import com.lionlink.backend.domain.auth.dto.request.SignupRequest;
import com.lionlink.backend.domain.auth.dto.response.GoogleInfoResponse;
import com.lionlink.backend.domain.auth.dto.response.MeResponse;
import com.lionlink.backend.domain.auth.dto.response.SessionResponse;
import com.lionlink.backend.domain.auth.event.SignupRequestedEvent;
import com.lionlink.backend.domain.auth.exception.AuthErrorCode;
import com.lionlink.backend.domain.profile.basic.entity.Profile;
import com.lionlink.backend.domain.profile.basic.repository.ProfileRepository;
import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	private final JwtProvider jwtProvider;
	private final RefreshTokenService refreshTokenService;
	private final UserRepository userRepository;
	private final ProfileRepository profileRepository;
	private final ApplicationEventPublisher eventPublisher;

	/** AUTH-01: 구글 로그인 성공 후 호출된다. 가입된 계정이면 리프레시 토큰, 아니면 가입용 토큰을 만든다. */
	@Transactional
	public GoogleLoginResult loginWithGoogle(GoogleIdentity identity) {
		return userRepository.findByGoogleSubAndDeletedAtIsNull(identity.sub())
				.map(user -> GoogleLoginResult.registered(refreshTokenService.issue(user.getId())))
				.orElseGet(() -> GoogleLoginResult.unregistered(jwtProvider.createSignupToken(identity)));
	}

	/** 가입 화면에 미리 채울 구글 이메일·이름. */
	public GoogleInfoResponse getSignupGoogleInfo(String signupToken) {
		return GoogleInfoResponse.from(jwtProvider.parseSignupToken(signupToken));
	}

	/** AUTH-02: 계정을 만들고(PENDING) 바로 로그인시킨다. */
	@Transactional
	public Issued<SessionResponse> signup(String signupToken, SignupRequest request) {
		GoogleIdentity identity = jwtProvider.parseSignupToken(signupToken);
		if (userRepository.existsByGoogleSub(identity.sub())) {
			throw new BusinessException(AuthErrorCode.ALREADY_REGISTERED);
		}

		User user = User.builder()
				.googleSub(identity.sub())
				.email(identity.email())
				.googleName(identity.name())
				.name(request.name().trim())
				.generation(request.generation())
				.build();
		try {
			userRepository.saveAndFlush(user);
		} catch (DataIntegrityViolationException e) {
			// 창 두 개에서 동시에 신청하면 google_sub 유니크 제약에 걸려 하나만 성공한다.
			throw new BusinessException(AuthErrorCode.ALREADY_REGISTERED);
		}

		eventPublisher.publishEvent(new SignupRequestedEvent(user.getId(), user.getName(), user.getGeneration()));
		return new Issued<>(
				new SessionResponse(
						jwtProvider.createAccessToken(user.getId()),
						jwtProvider.accessTokenExpiresInSeconds(),
						toMe(user)),
				refreshTokenService.issue(user.getId()));
	}

	private MeResponse toMe(User user) {
		String photoUrl = profileRepository.findById(user.getId()).map(Profile::getPhotoUrl).orElse(null);
		return MeResponse.of(user, photoUrl);
	}
}
