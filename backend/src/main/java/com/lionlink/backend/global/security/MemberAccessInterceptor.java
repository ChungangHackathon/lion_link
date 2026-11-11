package com.lionlink.backend.global.security;

import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class MemberAccessInterceptor implements HandlerInterceptor {

	private final ObjectProvider<CurrentUserProvider> currentUserProvider;
	private final MemberAccessReader memberAccessReader;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (!(handler instanceof HandlerMethod handlerMethod)) {
			return true;
		}
		CurrentUserProvider provider = currentUserProvider.getIfAvailable();
		Optional<Long> userId = provider == null ? Optional.empty() : provider.findUserId(request);
		if (userId.isEmpty()) {
			return true;
		}

		MemberAccess access = memberAccessReader.find(userId.get())
				.orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHENTICATED));
		if (!access.approved()) {
			throw new BusinessException(CommonErrorCode.NOT_APPROVED);
		}
		if (requiresCompletedProfile(handlerMethod) && !access.profileCompleted()) {
			throw new BusinessException(CommonErrorCode.PROFILE_NOT_COMPLETED);
		}
		return true;
	}

	private boolean requiresCompletedProfile(HandlerMethod handlerMethod) {
		return handlerMethod.hasMethodAnnotation(RequireCompletedProfile.class)
				|| handlerMethod.getBeanType().isAnnotationPresent(RequireCompletedProfile.class);
	}
}
