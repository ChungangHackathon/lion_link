package com.lionlink.backend.global.config;

import com.lionlink.backend.domain.admin.config.AdminAccessInterceptor;
import com.lionlink.backend.global.security.CurrentUserArgumentResolver;
import com.lionlink.backend.global.security.MemberAccessInterceptor;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

	public static final String API_PREFIX = "/api/v1";
	private static final String DOMAIN_PACKAGE = "com.lionlink.backend.domain";

	private static final List<String> UNAPPROVED_ALLOWED_PATHS = List.of(
			API_PREFIX + "/auth/**",
			API_PREFIX + "/me",
			API_PREFIX + "/me/signup-info",
			API_PREFIX + "/me/reapply",
			API_PREFIX + "/me/withdraw",
			API_PREFIX + "/email-notifications/unsubscribe"
	);

	private final CurrentUserArgumentResolver currentUserArgumentResolver;
	private final MemberAccessInterceptor memberAccessInterceptor;
	private final AdminAccessInterceptor adminAccessInterceptor;

	@Override
	public void configurePathMatch(PathMatchConfigurer configurer) {
		configurer.addPathPrefix(API_PREFIX, HandlerTypePredicate.forBasePackage(DOMAIN_PACKAGE));
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(currentUserArgumentResolver);
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(memberAccessInterceptor)
				.addPathPatterns(API_PREFIX + "/**")
				.excludePathPatterns(UNAPPROVED_ALLOWED_PATHS);
		// 등록 순서대로 실행된다: 승인 여부(NOT_APPROVED) 다음에 관리자 여부(FORBIDDEN).
		registry.addInterceptor(adminAccessInterceptor)
				.addPathPatterns(API_PREFIX + "/admin/**");
	}
}
