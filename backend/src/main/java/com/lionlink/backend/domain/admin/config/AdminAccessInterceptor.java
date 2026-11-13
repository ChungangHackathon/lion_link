package com.lionlink.backend.domain.admin.config;

import com.lionlink.backend.domain.user.entity.UserRole;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.CommonErrorCode;
import com.lionlink.backend.global.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 모든 /admin/** API의 관리자 검사. WebConfig가 MemberAccessInterceptor 뒤에 등록하므로
 * 승인 전 회원은 이 검사에 닿기 전에 NOT_APPROVED로 막힌다.
 */
@Component
@RequiredArgsConstructor
public class AdminAccessInterceptor implements HandlerInterceptor {

	private final ObjectProvider<CurrentUserProvider> currentUserProvider;
	private final UserRepository userRepository;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (!(handler instanceof HandlerMethod)) {
			return true;
		}
		CurrentUserProvider provider = currentUserProvider.getIfAvailable();
		Long userId = provider == null ? null : provider.findUserId(request).orElse(null);
		if (userId == null) {
			throw new BusinessException(CommonErrorCode.UNAUTHENTICATED);
		}
		if (!userRepository.existsByIdAndRole(userId, UserRole.ADMIN)) {
			throw new BusinessException(CommonErrorCode.FORBIDDEN);
		}
		return true;
	}
}
