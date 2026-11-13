package com.lionlink.backend.domain.profile.basic.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.lionlink.backend.global.security.DevHeaderCurrentUserProvider;
import com.lionlink.backend.support.AuthIntegrationTest;
import com.lionlink.backend.support.TestUsers;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** PROFILE-04·06~10 */
class ProfileEditControllerTest extends AuthIntegrationTest {

	private static final String BASIC = "/api/v1/me/profile/basic";
	private static final String BIO = "/api/v1/me/profile/bio";
	private static final String SKILLS = "/api/v1/me/profile/skills";
	private static final String LINKS = "/api/v1/me/profile/links";
	private static final String CONTACT = "/api/v1/me/profile/contact";
	private static final String VISIBILITY = "/api/v1/me/profile/visibility";

	private static final String STUDENT_BODY = "{'lionParts':['BACKEND'],'status':'STUDENT','interests':['BACKEND']}";
	private static final String EMPLOYED_BODY = """
			{'lionParts':['FRONTEND','BACKEND'],'status':'EMPLOYED','interests':['BACKEND','DATA_AI'],
			 'company':'핀테크A','jobTitle':'백엔드 개발자','link':'https://github.com/lion','headline':'핀테크 백엔드 개발자'}""";

	// ---------- PROFILE-04 기본 정보 ----------

	@Test
	void 기본_정보를_보낸_값으로_통째로_교체한다() throws Exception {
		Session lion = onboarded(EMPLOYED_BODY);

		putJson(lion, BASIC, """
				{'headline':'  데이터 엔지니어  ','major':'컴퓨터공학','minor':'경영학','region':'서울 강남',
				 'status':'GRAD_SCHOOL','lionParts':['DESIGN'],'interests':['DATA_AI','STARTUP','ETC']}""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.headline").value("데이터 엔지니어"))
				.andExpect(jsonPath("$.major").value("컴퓨터공학"))
				.andExpect(jsonPath("$.minor").value("경영학"))
				.andExpect(jsonPath("$.region").value("서울 강남"))
				.andExpect(jsonPath("$.status").value("GRAD_SCHOOL"))
				.andExpect(jsonPath("$.lionParts", contains("DESIGN")))
				.andExpect(jsonPath("$.interests", contains("DATA_AI", "STARTUP", "ETC")));

		getMyProfile(lion)
				.andExpect(jsonPath("$.headline").value("데이터 엔지니어"))
				.andExpect(jsonPath("$.major").value("컴퓨터공학"))
				.andExpect(jsonPath("$.region").value("서울 강남"))
				.andExpect(jsonPath("$.status").value("GRAD_SCHOOL"))
				.andExpect(jsonPath("$.lionParts", contains("DESIGN")))
				.andExpect(jsonPath("$.interests", contains("DATA_AI", "STARTUP", "ETC")));
	}

	@Test
	void 파트를_2개에서_1개로_줄이면_두번째_파트가_사라진다() throws Exception {
		Session lion = onboarded(EMPLOYED_BODY);

		putJson(lion, BASIC, "{'status':'EMPLOYED','lionParts':['BACKEND'],'interests':['BACKEND']}")
				.andExpect(status().isOk());

		getMyProfile(lion).andExpect(jsonPath("$.lionParts", contains("BACKEND")));
	}

	@Test
	void 선택_칸을_비우면_null이_된다() throws Exception {
		Session lion = onboarded(EMPLOYED_BODY);
		putJson(lion, BASIC, """
				{'headline':'a','major':'b','minor':'c','region':'d',
				 'status':'STUDENT','lionParts':['BACKEND'],'interests':['BACKEND']}""").andExpect(status().isOk());

		putJson(lion, BASIC, """
				{'headline':'  ','major':'','minor':null,
				 'status':'STUDENT','lionParts':['BACKEND'],'interests':['BACKEND']}""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.headline").value(nullValue()))
				.andExpect(jsonPath("$.major").value(nullValue()))
				.andExpect(jsonPath("$.minor").value(nullValue()))
				.andExpect(jsonPath("$.region").value(nullValue()));

		getMyProfile(lion)
				.andExpect(jsonPath("$.headline").value(nullValue()))
				.andExpect(jsonPath("$.region").value(nullValue()));
	}

	@Test
	void 재직에서_다른_상태로_바꿔도_경력은_그대로다() throws Exception {
		Session lion = onboarded(EMPLOYED_BODY);

		putJson(lion, BASIC, "{'status':'STUDENT','lionParts':['BACKEND'],'interests':['BACKEND']}")
				.andExpect(status().isOk());

		getMyProfile(lion)
				.andExpect(jsonPath("$.status").value("STUDENT"))
				.andExpect(jsonPath("$.careers", hasSize(1)))
				.andExpect(jsonPath("$.careers[0].company").value("핀테크A"));
	}

	@Test
	void 재직으로_바꿔도_경력을_만들지_않는다() throws Exception {
		Session lion = onboarded(STUDENT_BODY);

		putJson(lion, BASIC, "{'status':'EMPLOYED','lionParts':['BACKEND'],'interests':['BACKEND']}")
				.andExpect(status().isOk());

		getMyProfile(lion)
				.andExpect(jsonPath("$.status").value("EMPLOYED"))
				.andExpect(jsonPath("$.careers", hasSize(0)));
	}

	@Test
	void 기본_정보를_바꿔도_멋사_활동_파트는_그대로다() throws Exception {
		Session lion = onboarded(EMPLOYED_BODY);

		putJson(lion, BASIC, "{'status':'EMPLOYED','lionParts':['DESIGN'],'interests':['BACKEND']}")
				.andExpect(status().isOk());

		getMyProfile(lion)
				.andExpect(jsonPath("$.lionActivities[0].isDefault").value(true))
				.andExpect(jsonPath("$.lionActivities[0].parts", contains("FRONTEND", "BACKEND")));
	}

	static Stream<Arguments> 잘못된_기본_정보() {
		String over = "가".repeat(101);
		return Stream.of(
				Arguments.of("한 줄 소개 101자", "{'headline':'" + over + "','status':'STUDENT','lionParts':['BACKEND'],'interests':['BACKEND']}", "headline"),
				Arguments.of("주전공 31자", "{'major':'" + "가".repeat(31) + "','status':'STUDENT','lionParts':['BACKEND'],'interests':['BACKEND']}", "major"),
				Arguments.of("복수전공 31자", "{'minor':'" + "가".repeat(31) + "','status':'STUDENT','lionParts':['BACKEND'],'interests':['BACKEND']}", "minor"),
				Arguments.of("지역 21자", "{'region':'" + "가".repeat(21) + "','status':'STUDENT','lionParts':['BACKEND'],'interests':['BACKEND']}", "region"),
				Arguments.of("상태 없음", "{'lionParts':['BACKEND'],'interests':['BACKEND']}", "status"),
				Arguments.of("파트 없음", "{'status':'STUDENT','interests':['BACKEND']}", "lionParts"),
				Arguments.of("파트 빈 배열", "{'status':'STUDENT','lionParts':[],'interests':['BACKEND']}", "lionParts"),
				Arguments.of("파트 3개", "{'status':'STUDENT','lionParts':['FRONTEND','BACKEND','DESIGN'],'interests':['BACKEND']}", "lionParts"),
				Arguments.of("파트 중복", "{'status':'STUDENT','lionParts':['BACKEND','BACKEND'],'interests':['BACKEND']}", "lionParts"),
				Arguments.of("관심 분야 없음", "{'status':'STUDENT','lionParts':['BACKEND']}", "interests"),
				Arguments.of("관심 분야 4개", "{'status':'STUDENT','lionParts':['BACKEND'],'interests':['BACKEND','DESIGN','APP','ETC']}", "interests"),
				Arguments.of("관심 분야 중복", "{'status':'STUDENT','lionParts':['BACKEND'],'interests':['BACKEND','BACKEND']}", "interests"));
	}

	@ParameterizedTest(name = "{0}이면 400")
	@MethodSource("잘못된_기본_정보")
	void 잘못된_기본_정보는_400_VALIDATION_ERROR(String ignored, String body, String field) throws Exception {
		Session lion = onboarded(STUDENT_BODY);

		putJson(lion, BASIC, body)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value(field));
	}

	@ParameterizedTest(name = "{0}이면 400")
	@MethodSource("없는_열거값")
	void 없는_열거값은_400_VALIDATION_ERROR(String ignored, String body) throws Exception {
		Session lion = onboarded(STUDENT_BODY);

		putJson(lion, BASIC, body)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	static Stream<Arguments> 없는_열거값() {
		return Stream.of(
				Arguments.of("상태", "{'status':'RETIRED','lionParts':['BACKEND'],'interests':['BACKEND']}"),
				Arguments.of("파트", "{'status':'STUDENT','lionParts':['COOKING'],'interests':['BACKEND']}"),
				Arguments.of("관심 분야", "{'status':'STUDENT','lionParts':['BACKEND'],'interests':['COOKING']}"));
	}

	// ---------- PROFILE-06 소개 ----------

	@Test
	void 소개를_보낸_값으로_교체하고_줄바꿈을_유지한다() throws Exception {
		Session lion = onboarded(STUDENT_BODY);

		putJson(lion, BIO, "{'bio':'첫 줄\\n둘째 줄'}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.bio").value("첫 줄\n둘째 줄"));
		putJson(lion, BIO, "{'bio':'새 소개'}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.bio").value("새 소개"));

		getMyProfile(lion).andExpect(jsonPath("$.bio").value("새 소개"));
	}

	@Test
	void 소개를_null이나_빈_문자열로_보내면_비워진다() throws Exception {
		Session lion = onboarded(STUDENT_BODY);
		putJson(lion, BIO, "{'bio':'소개'}").andExpect(status().isOk());

		putJson(lion, BIO, "{'bio':''}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.bio").value(nullValue()));
		putJson(lion, BIO, "{'bio':'소개'}").andExpect(status().isOk());
		putJson(lion, BIO, "{'bio':null}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.bio").value(nullValue()));

		getMyProfile(lion).andExpect(jsonPath("$.bio").value(nullValue()));
	}

	@Test
	void 소개가_1000자까지는_되고_1001자는_400() throws Exception {
		Session lion = onboarded(STUDENT_BODY);

		putJson(lion, BIO, "{'bio':'" + "가".repeat(1000) + "'}").andExpect(status().isOk());
		putJson(lion, BIO, "{'bio':'" + "가".repeat(1001) + "'}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("bio"));
	}

	// ---------- PROFILE-07 대표 기술 ----------

	@Test
	void 대표_기술을_보낸_순서대로_통째로_교체한다() throws Exception {
		Session lion = onboarded(STUDENT_BODY);

		putJson(lion, SKILLS, "{'skills':['Spring','MySQL','AWS']}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.skills", contains("Spring", "MySQL", "AWS")));
		putJson(lion, SKILLS, "{'skills':['React','Spring']}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.skills", contains("React", "Spring")));

		getMyProfile(lion).andExpect(jsonPath("$.skills", contains("React", "Spring")));
	}

	@Test
	void 대표_기술의_중복은_앞쪽만_남기고_순서를_유지한다() throws Exception {
		Session lion = onboarded(STUDENT_BODY);

		putJson(lion, SKILLS, "{'skills':['Spring','MySQL',' Spring ','AWS','MySQL']}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.skills", contains("Spring", "MySQL", "AWS")));
	}

	@Test
	void 대표_기술을_빈_배열이나_null로_보내면_비워진다() throws Exception {
		Session lion = onboarded(STUDENT_BODY);
		putJson(lion, SKILLS, "{'skills':['Spring']}").andExpect(status().isOk());

		putJson(lion, SKILLS, "{'skills':[]}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.skills", hasSize(0)));
		getMyProfile(lion).andExpect(jsonPath("$.skills", hasSize(0)));

		putJson(lion, SKILLS, "{'skills':['Spring']}").andExpect(status().isOk());
		putJson(lion, SKILLS, "{'skills':null}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.skills", hasSize(0)));
	}

	@Test
	void 대표_기술은_5개까지_되고_6개는_400() throws Exception {
		Session lion = onboarded(STUDENT_BODY);

		putJson(lion, SKILLS, "{'skills':['a','b','c','d','e']}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.skills", hasSize(5)));
		putJson(lion, SKILLS, "{'skills':['a','b','c','d','e','f']}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("skills"));
	}

	@Test
	void 중복을_지우면_5개가_되더라도_보낸_개수가_6개면_400() throws Exception {
		Session lion = onboarded(STUDENT_BODY);

		putJson(lion, SKILLS, "{'skills':['a','b','c','d','e','a']}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void 대표_기술_태그가_20자_초과이거나_비었으면_400() throws Exception {
		Session lion = onboarded(STUDENT_BODY);

		putJson(lion, SKILLS, "{'skills':['" + "a".repeat(20) + "']}").andExpect(status().isOk());
		putJson(lion, SKILLS, "{'skills':['" + "a".repeat(21) + "']}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		putJson(lion, SKILLS, "{'skills':['Spring','  ']}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		putJson(lion, SKILLS, "{'skills':['Spring',null]}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	// ---------- PROFILE-08 외부 링크 ----------

	@Test
	void 외부_링크를_보낸_순서대로_통째로_교체한다() throws Exception {
		Session lion = onboarded(EMPLOYED_BODY);

		putJson(lion, LINKS, """
				{'links':[{'type':'BLOG','url':'https://blog.example.com'},
				          {'type':'GITHUB','url':'https://github.com/lion'},
				          {'type':'BLOG','url':'http://second.example.com'}]}""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.links", hasSize(3)))
				.andExpect(jsonPath("$.links[0].type").value("BLOG"))
				.andExpect(jsonPath("$.links[0].url").value("https://blog.example.com"))
				.andExpect(jsonPath("$.links[1].type").value("GITHUB"))
				.andExpect(jsonPath("$.links[2].url").value("http://second.example.com"));

		getMyProfile(lion)
				.andExpect(jsonPath("$.links", hasSize(3)))
				.andExpect(jsonPath("$.links[0].url").value("https://blog.example.com"))
				.andExpect(jsonPath("$.links[1].type").value("GITHUB"));
	}

	@Test
	void 외부_링크를_빈_배열이나_null로_보내면_비워진다() throws Exception {
		Session lion = onboarded(EMPLOYED_BODY);

		putJson(lion, LINKS, "{'links':[]}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.links", hasSize(0)));
		getMyProfile(lion).andExpect(jsonPath("$.links", hasSize(0)));

		putJson(lion, LINKS, "{'links':[{'type':'NOTION','url':'https://notion.so/lion'}]}").andExpect(status().isOk());
		putJson(lion, LINKS, "{'links':null}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.links", hasSize(0)));
	}

	@Test
	void 외부_링크는_5개까지_되고_6개는_400() throws Exception {
		Session lion = onboarded(STUDENT_BODY);
		String item = "{'type':'ETC','url':'https://example.com'}";

		putJson(lion, LINKS, "{'links':[" + String.join(",", java.util.Collections.nCopies(5, item)) + "]}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.links", hasSize(5)));
		putJson(lion, LINKS, "{'links':[" + String.join(",", java.util.Collections.nCopies(6, item)) + "]}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("links"));
	}

	static Stream<Arguments> 잘못된_링크() {
		return Stream.of(
				Arguments.of("없는 종류", "{'links':[{'type':'FACEBOOK','url':'https://example.com'}]}"),
				Arguments.of("종류 없음", "{'links':[{'url':'https://example.com'}]}"),
				Arguments.of("주소 없음", "{'links':[{'type':'BLOG'}]}"),
				Arguments.of("http가 아닌 주소", "{'links':[{'type':'BLOG','url':'ftp://example.com'}]}"),
				Arguments.of("스킴 없는 주소", "{'links':[{'type':'BLOG','url':'example.com'}]}"),
				Arguments.of("공백이 든 주소", "{'links':[{'type':'BLOG','url':'https://exa mple.com'}]}"),
				Arguments.of("500자 초과 주소", "{'links':[{'type':'BLOG','url':'https://e.com/" + "a".repeat(490) + "'}]}"),
				Arguments.of("빈 항목", "{'links':[null]}"));
	}

	@ParameterizedTest(name = "{0}이면 400")
	@MethodSource("잘못된_링크")
	void 잘못된_링크는_400_VALIDATION_ERROR(String ignored, String body) throws Exception {
		Session lion = onboarded(STUDENT_BODY);

		putJson(lion, LINKS, body)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	// ---------- PROFILE-09 연락처 ----------

	@Test
	void 연락처를_보낸_값으로_통째로_교체한다() throws Exception {
		Session lion = onboarded(STUDENT_BODY);

		putJson(lion, CONTACT, "{'email':'pub@example.com','phone':'010-1234-5678'}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("pub@example.com"))
				.andExpect(jsonPath("$.phone").value("010-1234-5678"));
		putJson(lion, CONTACT, "{'email':'other@example.com'}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("other@example.com"))
				.andExpect(jsonPath("$.phone").value(nullValue()));

		getMyProfile(lion)
				.andExpect(jsonPath("$.contact.email").value("other@example.com"))
				.andExpect(jsonPath("$.contact.phone").value(nullValue()));
	}

	@Test
	void 전화번호가_빈_문자열이면_null로_저장된다() throws Exception {
		Session lion = onboarded(STUDENT_BODY);
		putJson(lion, CONTACT, "{'email':'a@example.com','phone':'010-1234-5678'}").andExpect(status().isOk());

		putJson(lion, CONTACT, "{'email':'a@example.com','phone':''}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.phone").value(nullValue()));
		getMyProfile(lion).andExpect(jsonPath("$.contact.phone").value(nullValue()));
	}

	@Test
	void 연락처를_바꿔도_가입_이메일은_그대로다() throws Exception {
		Session lion = onboarded(STUDENT_BODY);
		String signupEmail = JsonPath.read(
				mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(lion)))
						.andReturn().getResponse().getContentAsString(), "$.email");

		putJson(lion, CONTACT, "{'email':'show@example.com'}").andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(lion)))
				.andExpect(jsonPath("$.email").value(signupEmail));
	}

	static Stream<Arguments> 잘못된_연락처() {
		return Stream.of(
				Arguments.of("이메일 없음", "{'phone':'010-1234-5678'}", "email"),
				Arguments.of("이메일 빈 값", "{'email':'  '}", "email"),
				Arguments.of("이메일 형식 오류", "{'email':'not-an-email'}", "email"),
				Arguments.of("이메일 256자", "{'email':'" + "a".repeat(250) + "@b.com'}", "email"),
				Arguments.of("전화번호 하이픈 없음", "{'email':'a@example.com','phone':'01012345678'}", "phone"),
				Arguments.of("전화번호 010이 아님", "{'email':'a@example.com','phone':'011-1234-5678'}", "phone"),
				Arguments.of("전화번호 자리수 오류", "{'email':'a@example.com','phone':'010-123-5678'}", "phone"));
	}

	@ParameterizedTest(name = "{0}이면 400")
	@MethodSource("잘못된_연락처")
	void 잘못된_연락처는_400_VALIDATION_ERROR(String ignored, String body, String field) throws Exception {
		Session lion = onboarded(STUDENT_BODY);

		putJson(lion, CONTACT, body)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value(field));
	}

	// ---------- PROFILE-10 공개 범위 ----------

	@Test
	void 공개_범위를_바꾸면_네_값을_모두_돌려준다() throws Exception {
		Session lion = onboarded(STUDENT_BODY);

		patchJson(lion, "{'email':'PUBLIC','phone':'PRIVATE','links':'CONNECTED','company':'PRIVATE'}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.visibility.email").value("PUBLIC"))
				.andExpect(jsonPath("$.visibility.phone").value("PRIVATE"))
				.andExpect(jsonPath("$.visibility.links").value("CONNECTED"))
				.andExpect(jsonPath("$.visibility.company").value("PRIVATE"));

		getMyProfile(lion)
				.andExpect(jsonPath("$.visibility.email").value("PUBLIC"))
				.andExpect(jsonPath("$.visibility.phone").value("PRIVATE"))
				.andExpect(jsonPath("$.visibility.links").value("CONNECTED"))
				.andExpect(jsonPath("$.visibility.company").value("PRIVATE"));
	}

	@Test
	void 보낸_키만_바꾸고_나머지는_그대로다() throws Exception {
		Session lion = onboarded(STUDENT_BODY);

		patchJson(lion, "{'phone':'PUBLIC'}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.visibility.email").value("CONNECTED"))
				.andExpect(jsonPath("$.visibility.phone").value("PUBLIC"))
				.andExpect(jsonPath("$.visibility.links").value("PUBLIC"))
				.andExpect(jsonPath("$.visibility.company").value("PUBLIC"));
		patchJson(lion, "{'company':'PRIVATE'}")
				.andExpect(jsonPath("$.visibility.phone").value("PUBLIC"))
				.andExpect(jsonPath("$.visibility.company").value("PRIVATE"));
		patchJson(lion, "{}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.visibility.phone").value("PUBLIC"))
				.andExpect(jsonPath("$.visibility.company").value("PRIVATE"));
	}

	@Test
	void 회사명에_CONNECTED를_보내면_400() throws Exception {
		Session lion = onboarded(STUDENT_BODY);

		patchJson(lion, "{'company':'CONNECTED'}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("company"));
		getMyProfile(lion).andExpect(jsonPath("$.visibility.company").value("PUBLIC"));
	}

	@ParameterizedTest(name = "{0}에 없는 값이면 400")
	@ValueSource(strings = {"email", "phone", "links", "company"})
	void 없는_공개_범위_값은_400(String key) throws Exception {
		Session lion = onboarded(STUDENT_BODY);

		patchJson(lion, "{'" + key + "':'SECRET'}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void 일부_키가_틀리면_다른_키도_저장되지_않는다() throws Exception {
		Session lion = onboarded(STUDENT_BODY);

		patchJson(lion, "{'email':'PRIVATE','company':'CONNECTED'}").andExpect(status().isBadRequest());

		getMyProfile(lion).andExpect(jsonPath("$.visibility.email").value("CONNECTED"));
	}

	// ---------- 다른 동문 조회에 반영 ----------

	@Test
	void 저장한_연락처와_공개_범위가_다른_동문의_상세_조회에_반영된다() throws Exception {
		Session lion = onboarded(STUDENT_BODY);
		setStatus(lion.userId(), "APPROVED");
		Long viewer = TestUsers.approved(entityManager, "보는사자");

		putJson(lion, CONTACT, "{'email':'show@example.com','phone':'010-1234-5678'}").andExpect(status().isOk());
		patchJson(lion, "{'email':'PUBLIC','phone':'CONNECTED'}").andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/users/{userId}", lion.userId()).header(DevHeaderCurrentUserProvider.HEADER, viewer))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contact.email").value("show@example.com"))
				.andExpect(jsonPath("$.contact.phone").doesNotExist());
	}

	@Test
	void 공개_범위를_비공개로_바꾸면_다른_동문에게_연락처가_가려진다() throws Exception {
		Session lion = onboarded(STUDENT_BODY);
		setStatus(lion.userId(), "APPROVED");
		Long viewer = TestUsers.approved(entityManager, "보는사자");
		putJson(lion, CONTACT, "{'email':'show@example.com','phone':'010-1234-5678'}").andExpect(status().isOk());
		patchJson(lion, "{'email':'PUBLIC','phone':'PUBLIC'}").andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/users/{userId}", lion.userId()).header(DevHeaderCurrentUserProvider.HEADER, viewer))
				.andExpect(jsonPath("$.contact.phone").value("010-1234-5678"));

		patchJson(lion, "{'phone':'PRIVATE'}").andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/users/{userId}", lion.userId()).header(DevHeaderCurrentUserProvider.HEADER, viewer))
				.andExpect(jsonPath("$.contact.email").value("show@example.com"))
				.andExpect(jsonPath("$.contact.phone").doesNotExist());
	}

	// ---------- 접근 제어 ----------

	static Stream<Arguments> 모든_수정_API() {
		return Stream.of(
				Arguments.of(BASIC, "{'status':'STUDENT','lionParts':['BACKEND'],'interests':['BACKEND']}"),
				Arguments.of(BIO, "{'bio':'소개'}"),
				Arguments.of(SKILLS, "{'skills':['Spring']}"),
				Arguments.of(LINKS, "{'links':[{'type':'BLOG','url':'https://example.com'}]}"),
				Arguments.of(CONTACT, "{'email':'a@example.com'}"),
				Arguments.of(VISIBILITY, "{'phone':'PUBLIC'}"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("모든_수정_API")
	void 승인_대기_회원도_수정할_수_있다(String path, String body) throws Exception {
		Session lion = onboarded(STUDENT_BODY);

		putJson(lion, path, body).andExpect(status().isOk());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("모든_수정_API")
	void 승인_회원도_수정할_수_있다(String path, String body) throws Exception {
		Session lion = onboarded(STUDENT_BODY);
		setStatus(lion.userId(), "APPROVED");

		putJson(lion, path, body).andExpect(status().isOk());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("모든_수정_API")
	void 반려_회원은_403_NOT_APPROVED(String path, String body) throws Exception {
		Session lion = onboarded(STUDENT_BODY);
		reject(lion.userId(), "기수 정보를 확인할 수 없어요.");

		putJson(lion, path, body)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("NOT_APPROVED"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("모든_수정_API")
	void 온보딩_전이면_403_PROFILE_NOT_COMPLETED(String path, String body) throws Exception {
		Session lion = signup("lion");

		putJson(lion, path, body)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PROFILE_NOT_COMPLETED"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("모든_수정_API")
	void 토큰이_없으면_401_UNAUTHENTICATED(String path, String body) throws Exception {
		mockMvc.perform(requestFor(path).contentType(MediaType.APPLICATION_JSON).content(json(body)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	// ---------- helper ----------

	private Session onboarded(String body) throws Exception {
		Session lion = signup("lion");
		onboard(lion, body).andExpect(status().isOk());
		return lion;
	}

	private ResultActions putJson(Session session, String path, String body) throws Exception {
		return mockMvc.perform(requestFor(path)
				.header(HttpHeaders.AUTHORIZATION, bearer(session))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(body)));
	}

	private ResultActions getMyProfile(Session session) throws Exception {
		return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.get("/api/v1/me/profile").header(HttpHeaders.AUTHORIZATION, bearer(session)));
	}

	/** 공개 범위만 PATCH, 나머지는 PUT. */
	private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder requestFor(String path) {
		return path.equals(VISIBILITY) ? patch(path) : put(path);
	}

	private ResultActions patchJson(Session session, String body) throws Exception {
		return mockMvc.perform(patch(VISIBILITY)
				.header(HttpHeaders.AUTHORIZATION, bearer(session))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(body)));
	}
}
