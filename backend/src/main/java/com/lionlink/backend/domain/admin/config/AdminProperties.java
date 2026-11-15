package com.lionlink.backend.domain.admin.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 관리자 설정.
 *
 * @param emails 서버가 뜰 때 관리자로 지정할 이메일. 쉼표로 여러 개를 넣는다
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(List<String> emails) {

	public AdminProperties {
		emails = emails == null ? List.of()
				: emails.stream().map(String::trim).filter(email -> !email.isEmpty()).toList();
	}
}
