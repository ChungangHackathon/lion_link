package com.lionlink.backend.domain.community.post.dto.request;

final class PostRequestRules {

	static final String LINK = "^https?://\\S+$";
	static final String LINK_MESSAGE = "링크는 http:// 또는 https://로 시작해야 합니다.";
	static final String NOT_BLANK = "(?s).*\\S.*";

	private PostRequestRules() {
	}
}
