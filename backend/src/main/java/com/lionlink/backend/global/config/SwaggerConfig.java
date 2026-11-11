package com.lionlink.backend.global.config;

import com.lionlink.backend.global.security.CurrentUser;
import com.lionlink.backend.global.security.DevHeaderCurrentUserProvider;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

	private static final String DEV_USER_SCHEME = "devUser";

	static {
		SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUser.class);
	}

	@Bean
	public OpenAPI openAPI() {
		SecurityScheme devUser = new SecurityScheme()
				.type(SecurityScheme.Type.APIKEY)
				.in(SecurityScheme.In.HEADER)
				.name(DevHeaderCurrentUserProvider.HEADER)
				.description("개발용 로그인 회원 ID");
		return new OpenAPI()
				.info(new Info().title("LION:LINK API").version("v1"))
				.components(new Components().addSecuritySchemes(DEV_USER_SCHEME, devUser))
				.addSecurityItem(new SecurityRequirement().addList(DEV_USER_SCHEME));
	}
}
