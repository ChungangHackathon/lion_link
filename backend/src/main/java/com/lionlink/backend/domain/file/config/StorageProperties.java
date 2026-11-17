package com.lionlink.backend.domain.file.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
		String bucket,
		@DefaultValue("ap-northeast-2") String region,
		String accessKey,
		String secretKey,
		String endpoint,
		String publicUrl,
		@DefaultValue("10m") Duration uploadUrlTtl
) {

	public String publicBaseUrl() {
		if (StringUtils.hasText(publicUrl)) {
			return trimSlash(publicUrl);
		}
		if (StringUtils.hasText(endpoint)) {
			return trimSlash(endpoint) + "/" + bucket;
		}
		return "https://" + bucket + ".s3." + region + ".amazonaws.com";
	}

	public boolean hasStaticCredentials() {
		return StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey);
	}

	private static String trimSlash(String url) {
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}
}
