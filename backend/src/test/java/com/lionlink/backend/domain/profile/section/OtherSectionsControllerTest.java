package com.lionlink.backend.domain.profile.section;

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

/** PROFILE-11 멋사 프로젝트·자격증·수상·대외활동·언어 */
class OtherSectionsControllerTest extends ProfileSectionApiTest {

	private static final String LION_PROJECTS = "/api/v1/me/lion-projects";
	private static final String CERTIFICATIONS = "/api/v1/me/certifications";
	private static final String AWARDS = "/api/v1/me/awards";
	private static final String ACTIVITIES = "/api/v1/me/activities";
	private static final String LANGUAGES = "/api/v1/me/languages";

	// ---------- 멋사 프로젝트·해커톤 ----------

	@Test
	void 멋사_프로젝트를_추가_수정_삭제한다() throws Exception {
		Session lion = onboarded(STUDENT);

		long id = create(lion, LION_PROJECTS, """
				{'name':'중앙 해커톤','eventType':'UNION_HACKATHON','generation':13,'myRole':'백엔드',
				 'description':'우수상','link':'https://example.com'}""");
		myProfile(lion)
				.andExpect(jsonPath("$.lionProjects", hasSize(1)))
				.andExpect(jsonPath("$.lionProjects[0].eventType").value("UNION_HACKATHON"));

		putItem(lion, LION_PROJECTS, id, "{'name':'정기 프로젝트','eventType':'REGULAR','generation':12}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eventType").value("REGULAR"))
				.andExpect(jsonPath("$.generation").value(12))
				.andExpect(jsonPath("$.myRole").value(nullValue()))
				.andExpect(jsonPath("$.link").value(nullValue()));

		deleteItem(lion, LION_PROJECTS, id).andExpect(status().isNoContent());
		myProfile(lion).andExpect(jsonPath("$.lionProjects", hasSize(0)));
	}

	static Stream<Arguments> 잘못된_멋사_프로젝트_요청() {
		return Stream.of(
				Arguments.of("이름 누락", "{'eventType':'REGULAR','generation':13}", "name"),
				Arguments.of("종류 누락", "{'name':'a','generation':13}", "eventType"),
				Arguments.of("기수 15", "{'name':'a','eventType':'REGULAR','generation':15}", "generation"),
				Arguments.of("링크가 http가 아님", "{'name':'a','eventType':'REGULAR','generation':13,'link':'javascript:x'}", "link"),
				Arguments.of("없는 종류", "{'name':'a','eventType':'JOINT_HACKATHON','generation':13}", null));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("잘못된_멋사_프로젝트_요청")
	void 잘못된_멋사_프로젝트_요청은_400이다(String name, String body, String field) throws Exception {
		assertBadRequest(LION_PROJECTS, body, field);
	}

	// ---------- 자격증 ----------

	@Test
	void 자격증을_추가_수정_삭제한다() throws Exception {
		Session lion = onboarded(STUDENT);

		long id = create(lion, CERTIFICATIONS, "{'name':'정보처리기사','issuer':'한국산업인력공단','acquiredYm':'2025-05'}");
		myProfile(lion)
				.andExpect(jsonPath("$.certifications[0].issuer").value("한국산업인력공단"))
				.andExpect(jsonPath("$.certifications[0].acquiredYm").value("2025-05"));

		putItem(lion, CERTIFICATIONS, id, "{'name':'SQLD'}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("SQLD"))
				.andExpect(jsonPath("$.issuer").value(nullValue()))
				.andExpect(jsonPath("$.acquiredYm").value(nullValue()));

		deleteItem(lion, CERTIFICATIONS, id).andExpect(status().isNoContent());
		myProfile(lion).andExpect(jsonPath("$.certifications", hasSize(0)));
	}

	static Stream<Arguments> 잘못된_자격증_요청() {
		return Stream.of(
				Arguments.of("이름 누락", "{'issuer':'a'}", "name"),
				Arguments.of("이름 31자", "{'name':'" + "가".repeat(31) + "'}", "name"),
				Arguments.of("발급 기관 31자", "{'name':'a','issuer':'" + "가".repeat(31) + "'}", "issuer"),
				Arguments.of("취득 년월 형식 오류", "{'name':'a','acquiredYm':'2025/05'}", null));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("잘못된_자격증_요청")
	void 잘못된_자격증_요청은_400이다(String name, String body, String field) throws Exception {
		assertBadRequest(CERTIFICATIONS, body, field);
	}

	// ---------- 수상 ----------

	@Test
	void 수상을_추가_수정_삭제한다() throws Exception {
		Session lion = onboarded(STUDENT);

		long id = create(lion, AWARDS, "{'name':'대상','host':'멋쟁이사자처럼','awardedYm':'2026-02','description':'해커톤'}");
		myProfile(lion)
				.andExpect(jsonPath("$.awards[0].host").value("멋쟁이사자처럼"))
				.andExpect(jsonPath("$.awards[0].awardedYm").value("2026-02"));

		putItem(lion, AWARDS, id, "{'name':'우수상'}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("우수상"))
				.andExpect(jsonPath("$.host").value(nullValue()))
				.andExpect(jsonPath("$.description").value(nullValue()));

		deleteItem(lion, AWARDS, id).andExpect(status().isNoContent());
		myProfile(lion).andExpect(jsonPath("$.awards", hasSize(0)));
	}

	static Stream<Arguments> 잘못된_수상_요청() {
		return Stream.of(
				Arguments.of("이름 누락", "{'host':'a'}", "name"),
				Arguments.of("주최 31자", "{'name':'a','host':'" + "가".repeat(31) + "'}", "host"),
				Arguments.of("설명 101자", "{'name':'a','description':'" + "가".repeat(101) + "'}", "description"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("잘못된_수상_요청")
	void 잘못된_수상_요청은_400이다(String name, String body, String field) throws Exception {
		assertBadRequest(AWARDS, body, field);
	}

	// ---------- 대외활동 ----------

	@Test
	void 대외활동을_추가_수정_삭제한다() throws Exception {
		Session lion = onboarded(STUDENT);

		long id = create(lion, ACTIVITIES, """
				{'name':'동아리 회장','organization':'중앙대 개발 동아리','startYm':'2025-03','endYm':'2025-12'}""");
		myProfile(lion).andExpect(jsonPath("$.activities[0].organization").value("중앙대 개발 동아리"));

		putItem(lion, ACTIVITIES, id, "{'name':'봉사','startYm':'2025-01'}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("봉사"))
				.andExpect(jsonPath("$.organization").value(nullValue()))
				.andExpect(jsonPath("$.endYm").value(nullValue()));

		deleteItem(lion, ACTIVITIES, id).andExpect(status().isNoContent());
		myProfile(lion).andExpect(jsonPath("$.activities", hasSize(0)));
	}

	static Stream<Arguments> 잘못된_대외활동_요청() {
		return Stream.of(
				Arguments.of("이름 누락", "{'organization':'a'}", "name"),
				Arguments.of("이름 31자", "{'name':'" + "가".repeat(31) + "'}", "name"),
				Arguments.of("종료가 시작보다 빠름", "{'name':'a','startYm':'2025-05','endYm':'2025-01'}", "endYm"),
				Arguments.of("시작이 미래", "{'name':'a','startYm':'2999-01'}", "startYm"),
				Arguments.of("시작 없이 종료만", "{'name':'a','endYm':'2025-01'}", "startYm"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("잘못된_대외활동_요청")
	void 잘못된_대외활동_요청은_400이다(String name, String body, String field) throws Exception {
		assertBadRequest(ACTIVITIES, body, field);
	}

	// ---------- 언어 ----------

	@Test
	void 언어를_추가_수정_삭제한다() throws Exception {
		Session lion = onboarded(STUDENT);

		long id = create(lion, LANGUAGES, "{'language':' English ','level':'DAILY'}");
		myProfile(lion)
				.andExpect(jsonPath("$.languages[0].language").value("English"))
				.andExpect(jsonPath("$.languages[0].level").value("DAILY"));

		putItem(lion, LANGUAGES, id, "{'language':'English','level':'BUSINESS'}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.level").value("BUSINESS"));

		deleteItem(lion, LANGUAGES, id).andExpect(status().isNoContent());
		myProfile(lion).andExpect(jsonPath("$.languages", hasSize(0)));
	}

	@Test
	void 같은_언어는_대소문자를_무시하고_중복이다() throws Exception {
		Session lion = onboarded(STUDENT);
		create(lion, LANGUAGES, "{'language':'English','level':'DAILY'}");
		long japanese = create(lion, LANGUAGES, "{'language':'日本語','level':'BASIC'}");

		postItem(lion, LANGUAGES, "{'language':'english','level':'NATIVE'}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE_ITEM"));
		putItem(lion, LANGUAGES, japanese, "{'language':'ENGLISH','level':'BASIC'}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE_ITEM"));
		putItem(lion, LANGUAGES, japanese, "{'language':'日本語','level':'BUSINESS'}")
				.andExpect(status().isOk());
	}

	static Stream<Arguments> 잘못된_언어_요청() {
		return Stream.of(
				Arguments.of("언어 누락", "{'level':'BASIC'}", "language"),
				Arguments.of("언어 21자", "{'language':'" + "a".repeat(21) + "','level':'BASIC'}", "language"),
				Arguments.of("수준 누락", "{'language':'English'}", "level"),
				Arguments.of("없는 수준", "{'language':'English','level':'CONVERSATIONAL'}", null));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("잘못된_언어_요청")
	void 잘못된_언어_요청은_400이다(String name, String body, String field) throws Exception {
		assertBadRequest(LANGUAGES, body, field);
	}

	private void assertBadRequest(String path, String body, String field) throws Exception {
		Session lion = onboarded(STUDENT);
		var result = postItem(lion, path, body)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		if (field != null) {
			result.andExpect(jsonPath("$.fieldErrors[*].field", hasItem(field)));
		}
	}
}
