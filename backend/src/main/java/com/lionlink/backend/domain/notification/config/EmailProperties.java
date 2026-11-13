package com.lionlink.backend.domain.notification.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 이메일 알림 설정. SMTP 접속 정보는 spring.mail.* 에 둔다.
 *
 * @param enabled     false면 실제로 보내지 않고 로그만 남긴다
 * @param from        보내는 사람
 * @param linkBaseUrl 메일 안 버튼과 수신 거부 링크가 가리킬 프론트 주소
 * @param maxAttempts 실패하면 이 횟수까지 보낸다
 * @param retryDelay  실패한 메일을 다시 보내기까지 기다리는 시간
 * @param batchSize   한 번에 꺼내 보내는 메일 수
 */
@ConfigurationProperties(prefix = "app.email")
public record EmailProperties(
		@DefaultValue("false") boolean enabled,
		@DefaultValue("LION:LINK <no-reply@lionlink.local>") String from,
		@DefaultValue("http://localhost:3000") String linkBaseUrl,
		@DefaultValue("3") int maxAttempts,
		@DefaultValue("10m") Duration retryDelay,
		@DefaultValue("50") int batchSize
) {
}
