package com.lionlink.backend.domain.profile.section;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** PROFILE-11 학력(온보딩이 중앙대학교 항목 1개를 만든다)과 프로젝트 */
class EducationProjectControllerTest extends ProfileSectionApiTest {

	private static final String EDUCATIONS = "/api/v1/me/educations";
	private static final String PROJECTS = "/api/v1/me/projects";

	// ---------- 학력 ----------

	@Test
	void 학력을_추가_수정_삭제한다() throws Exception {
		Session lion = onboarded(STUDENT);

		long id = create(lion, EDUCATIONS, """
				{'school':'  서울대학교 ','major':'컴퓨터공학','degree':'MASTER',
				 'startYm':'2022-03','endYm':'2024-02','description':'석사'}""");
		myProfile(lion).andExpect(jsonPath("$.educations", hasSize(2)));

		putItem(lion, EDUCATIONS, id, """
				{'school':'KAIST','degree':'DOCTOR','startYm':'2024-03','isCurrent':true}""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.school").value("KAIST"))
				.andExpect(jsonPath("$.major").value(nullValue()))
				.andExpect(jsonPath("$.degree").value("DOCTOR"))
				.andExpect(jsonPath("$.endYm").value(nullValue()))
				.andExpect(jsonPath("$.isCurrent").value(true))
				.andExpect(jsonPath("$.description").value(nullValue()));

		deleteItem(lion, EDUCATIONS, id).andExpect(status().isNoContent());
		myProfile(lion).andExpect(jsonPath("$.educations", hasSize(1)));
	}

	@Test
	void 학교명만_보내도_추가된다() throws Exception {
		Session lion = onboarded(STUDENT);

		postItem(lion, EDUCATIONS, "{'school':'해외 대학'}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.school").value("해외 대학"))
				.andExpect(jsonPath("$.startYm").value(nullValue()))
				.andExpect(jsonPath("$.isCurrent").value(false));
	}

	static Stream<Arguments> 잘못된_학력_요청() {
		return Stream.of(
				Arguments.of("학교명 누락", "{'major':'a'}", "school"),
				Arguments.of("학교명 31자", "{'school':'" + "가".repeat(31) + "'}", "school"),
				Arguments.of("전공 31자", "{'school':'a','major':'" + "가".repeat(31) + "'}", "major"),
				Arguments.of("설명 201자", "{'school':'a','description':'" + "가".repeat(201) + "'}", "description"),
				Arguments.of("종료가 시작보다 빠름", "{'school':'a','startYm':'2024-05','endYm':'2024-01'}", "endYm"),
				Arguments.of("진행 중인데 종료 입력", "{'school':'a','startYm':'2024-05','endYm':'2024-06','isCurrent':true}", "endYm"),
				Arguments.of("시작이 미래", "{'school':'a','startYm':'2999-01'}", "startYm"),
				Arguments.of("시작 없이 종료만", "{'school':'a','endYm':'2024-01'}", "startYm"),
				Arguments.of("없는 학위", "{'school':'a','degree':'PHD'}", null));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("잘못된_학력_요청")
	void 잘못된_학력_요청은_400이다(String name, String body, String field) throws Exception {
		Session lion = onboarded(STUDENT);

		var result = postItem(lion, EDUCATIONS, body)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		if (field != null) {
			result.andExpect(jsonPath("$.fieldErrors[*].field", hasItem(field)));
		}
	}

	// ---------- 프로젝트 ----------

	@Test
	void 프로젝트를_추가_수정_삭제한다() throws Exception {
		Session lion = onboarded(STUDENT);

		long id = create(lion, PROJECTS, """
				{'name':'라이언 링크','startYm':'2026-09','isCurrent':true,'role':'백엔드',
				 'description':'동문 네트워킹','link':'https://github.com/lion/link','tags':['Spring',' React ']}""");
		myProfile(lion)
				.andExpect(jsonPath("$.projects", hasSize(1)))
				.andExpect(jsonPath("$.projects[0].tags", contains("Spring", "React")))
				.andExpect(jsonPath("$.projects[0].isCurrent").value(true));

		putItem(lion, PROJECTS, id, """
				{'name':'라이언 링크 v2','startYm':'2026-09','endYm':'2026-10','tags':[]}""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("라이언 링크 v2"))
				.andExpect(jsonPath("$.endYm").value("2026-10"))
				.andExpect(jsonPath("$.isCurrent").value(false))
				.andExpect(jsonPath("$.role").value(nullValue()))
				.andExpect(jsonPath("$.link").value(nullValue()))
				.andExpect(jsonPath("$.tags", empty()));

		deleteItem(lion, PROJECTS, id).andExpect(status().isNoContent());
		myProfile(lion).andExpect(jsonPath("$.projects", hasSize(0)));
	}

	static Stream<Arguments> 잘못된_프로젝트_요청() {
		return Stream.of(
				Arguments.of("이름 누락", "{'role':'a'}", "name"),
				Arguments.of("이름 31자", "{'name':'" + "가".repeat(31) + "'}", "name"),
				Arguments.of("역할 31자", "{'name':'a','role':'" + "가".repeat(31) + "'}", "role"),
				Arguments.of("설명 301자", "{'name':'a','description':'" + "가".repeat(301) + "'}", "description"),
				Arguments.of("링크가 http가 아님", "{'name':'a','link':'ftp://x.com'}", "link"),
				Arguments.of("태그 6개", "{'name':'a','tags':['1','2','3','4','5','6']}", "tags"),
				Arguments.of("태그 중복", "{'name':'a','tags':['Spring','Spring']}", "tags"),
				Arguments.of("태그 21자", "{'name':'a','tags':['" + "a".repeat(21) + "']}", null),
				Arguments.of("종료가 시작보다 빠름", "{'name':'a','startYm':'2024-05','endYm':'2024-01'}", "endYm"),
				Arguments.of("시작이 미래", "{'name':'a','startYm':'2999-01','isCurrent':true}", "startYm"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("잘못된_프로젝트_요청")
	void 잘못된_프로젝트_요청은_400이다(String name, String body, String field) throws Exception {
		Session lion = onboarded(STUDENT);

		var result = postItem(lion, PROJECTS, body)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		if (field != null) {
			result.andExpect(jsonPath("$.fieldErrors[*].field", hasItem(field)));
		}
	}
}
