package com.lionlink.backend.domain.auth.config;

import com.lionlink.backend.domain.auth.oauth.GoogleLoginFailureHandler;
import com.lionlink.backend.domain.auth.oauth.GoogleLoginSuccessHandler;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 구글 로그인(Authorization Code)만 Spring Security가 처리한다.
 * API 인증은 기존처럼 @CurrentUser·MemberAccessInterceptor가 담당하므로 모든 요청을 통과시킨다.
 *
 * <pre>
 * GET /oauth2/authorization/google  → 구글 로그인 화면으로 리다이렉트
 * GET /login/oauth2/code/google     → code 교환 후 GoogleLoginSuccessHandler / FailureHandler
 * </pre>
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private final AuthProperties authProperties;
	private final GoogleLoginSuccessHandler successHandler;
	private final GoogleLoginFailureHandler failureHandler;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
			ClientRegistrationRepository clientRegistrationRepository) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				// 로그인 결과는 세션에 저장하지 않는다. 서비스 인증은 JWT로 한다.
				.securityContext(context -> context
						.securityContextRepository(new RequestAttributeSecurityContextRepository()))
				.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
				.oauth2Login(oauth2 -> oauth2
						.authorizationEndpoint(endpoint -> endpoint
								.authorizationRequestResolver(authorizationRequestResolver(clientRegistrationRepository)))
						.successHandler(successHandler)
						.failureHandler(failureHandler));
		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(List.of(authProperties.frontendUrl()));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true); // refreshToken·signupToken 쿠키를 주고받기 위해 필요

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	/** 로그인할 때마다 구글 계정 선택 화면을 띄운다. */
	private DefaultOAuth2AuthorizationRequestResolver authorizationRequestResolver(
			ClientRegistrationRepository clientRegistrationRepository) {
		DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(
				clientRegistrationRepository, OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
		resolver.setAuthorizationRequestCustomizer(customizer -> customizer
				.additionalParameters(parameters -> parameters.put("prompt", "select_account")));
		return resolver;
	}
}
