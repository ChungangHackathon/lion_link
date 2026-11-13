package com.lionlink.backend.domain.profile.basic.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LinkTypeTest {

	@Test
	void 주소의_도메인으로_종류를_정한다() {
		assertEquals(LinkType.LINKEDIN, LinkType.detect("https://www.linkedin.com/in/lion"));
		assertEquals(LinkType.GITHUB, LinkType.detect("https://github.com/lion"));
		assertEquals(LinkType.NOTION, LinkType.detect("https://lion.notion.site/portfolio"));
		assertEquals(LinkType.NOTION, LinkType.detect("https://www.notion.so/lion"));
		assertEquals(LinkType.ETC, LinkType.detect("https://velog.io/@lion"));
	}

	@Test
	void 도메인_대소문자는_구분하지_않는다() {
		assertEquals(LinkType.GITHUB, LinkType.detect("https://GitHub.com/lion"));
	}

	@Test
	void 이름만_비슷한_도메인은_ETC다() {
		assertEquals(LinkType.ETC, LinkType.detect("https://evilgithub.com/lion"));
		assertEquals(LinkType.ETC, LinkType.detect("https://github.com.evil.io/lion"));
	}

	@Test
	void 주소를_해석할_수_없으면_ETC다() {
		assertEquals(LinkType.ETC, LinkType.detect("https://exa mple.com"));
	}
}
