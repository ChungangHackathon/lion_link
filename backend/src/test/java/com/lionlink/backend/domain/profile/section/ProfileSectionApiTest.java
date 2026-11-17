package com.lionlink.backend.domain.profile.section;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.lionlink.backend.support.AuthIntegrationTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** PROFILE-11 테스트 공통 도구. 본문은 작은따옴표 JSON. */
public abstract class ProfileSectionApiTest extends AuthIntegrationTest {

	public static final String STUDENT = "{'lionParts':['BACKEND'],'status':'STUDENT','interests':['BACKEND']}";
	public static final String EMPLOYED = """
			{'lionParts':['BACKEND'],'status':'EMPLOYED','interests':['BACKEND'],
			 'company':'핀테크A','jobTitle':'백엔드 개발자'}""";

	/** 가입(승인 대기) 후 온보딩까지 마친 회원. */
	protected Session onboarded(String sub, String onboardingBody) throws Exception {
		Session session = signup(sub);
		onboard(session, onboardingBody).andExpect(status().isOk());
		return session;
	}

	protected Session onboarded(String onboardingBody) throws Exception {
		return onboarded("lion", onboardingBody);
	}

	protected ResultActions postItem(Session session, String path, String body) throws Exception {
		return mockMvc.perform(post(path)
				.header(HttpHeaders.AUTHORIZATION, bearer(session))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(body)));
	}

	protected ResultActions putItem(Session session, String path, long itemId, String body) throws Exception {
		return mockMvc.perform(put(path + "/" + itemId)
				.header(HttpHeaders.AUTHORIZATION, bearer(session))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(body)));
	}

	protected ResultActions deleteItem(Session session, String path, long itemId) throws Exception {
		return mockMvc.perform(delete(path + "/" + itemId)
				.header(HttpHeaders.AUTHORIZATION, bearer(session)));
	}

	protected ResultActions myProfile(Session session) throws Exception {
		return mockMvc.perform(get("/api/v1/me/profile").header(HttpHeaders.AUTHORIZATION, bearer(session)));
	}

	/** 추가하고 만들어진 항목의 id를 돌려준다. */
	protected long create(Session session, String path, String body) throws Exception {
		String response = postItem(session, path, body).andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(response, "$.id")).longValue();
	}

	/** 내 프로필에서 JsonPath로 값을 읽는다. */
	protected <T> T fromProfile(Session session, String jsonPath) throws Exception {
		String response = myProfile(session).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, jsonPath);
	}
}
