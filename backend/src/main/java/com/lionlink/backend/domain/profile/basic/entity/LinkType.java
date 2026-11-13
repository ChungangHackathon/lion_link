package com.lionlink.backend.domain.profile.basic.entity;

import java.net.URI;
import java.util.Locale;

public enum LinkType {
	LINKEDIN,
	GITHUB,
	PORTFOLIO,
	BLOG,
	REMEMBER,
	NOTION,
	ETC;

	/** 온보딩(PROFILE-01)에서 주소만 보고 종류를 정한다. 판별할 수 없으면 ETC. */
	public static LinkType detect(String url) {
		String host = hostOf(url);
		if (host == null) {
			return ETC;
		}
		if (isDomain(host, "linkedin.com")) {
			return LINKEDIN;
		}
		if (isDomain(host, "github.com")) {
			return GITHUB;
		}
		if (isDomain(host, "notion.site") || isDomain(host, "notion.so")) {
			return NOTION;
		}
		return ETC;
	}

	private static String hostOf(String url) {
		try {
			String host = URI.create(url.trim()).getHost();
			return host == null ? null : host.toLowerCase(Locale.ROOT);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/** host가 domain 자체이거나 그 하위 도메인(www.github.com)일 때만 true. evilgithub.com은 false. */
	private static boolean isDomain(String host, String domain) {
		return host.equals(domain) || host.endsWith("." + domain);
	}
}
