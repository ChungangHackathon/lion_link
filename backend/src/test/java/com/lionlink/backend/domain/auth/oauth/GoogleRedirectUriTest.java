package com.lionlink.backend.domain.auth.oauth;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionlink.backend.support.AuthIntegrationTest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;

/**
 * 구글 로그인 리디렉션 URI(OAUTH_REDIRECT_BASE_URL).
 * 운영은 프론트(Vercel)가 백엔드를 프록시하므로 구글에 보내는 redirect_uri가 프론트 주소여야 한다.
 */
class GoogleRedirectUriTest {

	private static final String KEY = "spring.security.oauth2.client.registration.google.redirect-uri";

	/** 테스트용 application.yml이 main 파일을 가리므로, main 파일을 직접 읽어 플레이스홀더를 풀어 본다. */
	private static String resolveMainYml(Map<String, Object> environment) throws IOException {
		List<PropertySource<?>> sources = new YamlPropertySourceLoader()
				.load("main", new FileSystemResource("src/main/resources/application.yml"));
		String raw = (String) sources.get(0).getProperty(KEY);
		StandardEnvironment env = new StandardEnvironment();
		env.getPropertySources().addFirst(new MapPropertySource("test", environment));
		return env.resolvePlaceholders(raw);
	}

	@Test
	void main_설정은_환경변수가_없으면_요청_주소_기준_템플릿이다() throws Exception {
		assertEquals("{baseUrl}/login/oauth2/code/{registrationId}", resolveMainYml(Map.of()));
	}

	@Test
	void main_설정은_환경변수가_있으면_그_주소로_고정된다() throws Exception {
		assertEquals("https://lion-link.vercel.app/login/oauth2/code/{registrationId}",
				resolveMainYml(Map.of("OAUTH_REDIRECT_BASE_URL", "https://lion-link.vercel.app")));
	}

	@SpringBootTest
	@AutoConfigureMockMvc
	static class WithoutEnv extends AuthIntegrationTest {

		@Test
		void 환경변수가_없으면_인가_요청의_redirect_uri는_요청_주소_기준이다() throws Exception {
			mockMvc.perform(get("/oauth2/authorization/google"))
					.andExpect(status().is3xxRedirection())
					.andExpect(header().string(HttpHeaders.LOCATION,
							containsString("redirect_uri=http://localhost/login/oauth2/code/google")));
		}
	}

	@SpringBootTest(properties = "OAUTH_REDIRECT_BASE_URL=https://lion-link.vercel.app")
	@AutoConfigureMockMvc
	static class WithEnv extends AuthIntegrationTest {

		@Test
		void 환경변수가_있으면_인가_요청의_redirect_uri는_그_주소다() throws Exception {
			mockMvc.perform(get("/oauth2/authorization/google"))
					.andExpect(status().is3xxRedirection())
					.andExpect(header().string(HttpHeaders.LOCATION,
							containsString("redirect_uri=https://lion-link.vercel.app/login/oauth2/code/google")));
		}
	}
}
