package com.lionlink.backend.domain.profile.section;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.function.IntFunction;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;

/** PROFILE-11 모든 목록형 섹션에 똑같이 적용되는 규칙. 섹션이 늘면 {@link #sections()}에 한 줄 추가. */
class SectionCommonRulesTest extends ProfileSectionApiTest {

	/** body는 i번째 항목의 유효한 요청 본문(서로 달라야 중복 규칙에 안 걸린다). max는 섹션 최대 개수. */
	record Spec(String name, String path, String key, int max, IntFunction<String> body) {

		@Override
		public String toString() {
			return name;
		}
	}

	static Stream<Arguments> sections() {
		return Stream.of(
				new Spec("careers", "/api/v1/me/careers", "careers", 10,
						i -> "{'company':'회사" + i + "','jobTitle':'a','employmentType':'ETC','startYm':'2020-01','isCurrent':true}"),
				new Spec("lion-activities", "/api/v1/me/lion-activities", "lionActivities", 6,
						i -> "{'generation':" + (i + 1) + ",'role':'LION','parts':['BACKEND']}"),
				new Spec("educations", "/api/v1/me/educations", "educations", 5,
						i -> "{'school':'학교" + i + "'}"),
				new Spec("lion-projects", "/api/v1/me/lion-projects", "lionProjects", 5,
						i -> "{'name':'프로젝트" + i + "','eventType':'REGULAR','generation':13}"),
				new Spec("projects", "/api/v1/me/projects", "projects", 10,
						i -> "{'name':'프로젝트" + i + "'}"),
				new Spec("certifications", "/api/v1/me/certifications", "certifications", 10,
						i -> "{'name':'자격증" + i + "'}"),
				new Spec("awards", "/api/v1/me/awards", "awards", 10,
						i -> "{'name':'수상" + i + "'}"),
				new Spec("activities", "/api/v1/me/activities", "activities", 10,
						i -> "{'name':'활동" + i + "'}"),
				new Spec("languages", "/api/v1/me/languages", "languages", 5,
						i -> "{'language':'언어" + i + "','level':'BASIC'}"))
				.map(Arguments::of);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("sections")
	void 최대_개수를_넘기면_409다(Spec spec) throws Exception {
		Session lion = onboarded(STUDENT);

		int created = 0;
		for (int i = 0; i <= spec.max(); i++) {
			var result = postItem(lion, spec.path(), spec.body().apply(i));
			if (result.andReturn().getResponse().getStatus() == 201) {
				created++;
				continue;
			}
			result.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("LIMIT_EXCEEDED"));
			break;
		}

		org.junit.jupiter.api.Assertions.assertTrue(created > 0 && created <= spec.max());
		myProfile(lion).andExpect(jsonPath("$." + spec.key(), hasSize(spec.max())));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("sections")
	void 남의_항목과_없는_항목은_수정_삭제_모두_404다(Spec spec) throws Exception {
		Session lion = onboarded("lion", STUDENT);
		Session other = onboarded("other", STUDENT);
		long id = create(lion, spec.path(), spec.body().apply(0));

		putItem(other, spec.path(), id, spec.body().apply(1))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
		deleteItem(other, spec.path(), id)
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
		putItem(lion, spec.path(), 999999, spec.body().apply(1))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
		deleteItem(lion, spec.path(), 999999)
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));

		myProfile(lion).andExpect(jsonPath("$." + spec.key() + "[?(@.id==" + id + ")]", hasSize(1)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("sections")
	void 반려된_회원은_403_NOT_APPROVED다(Spec spec) throws Exception {
		Session lion = onboarded(STUDENT);
		reject(lion.userId(), "사유");

		postItem(lion, spec.path(), spec.body().apply(0))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("NOT_APPROVED"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("sections")
	void 프로필을_만들기_전에는_403_PROFILE_NOT_COMPLETED다(Spec spec) throws Exception {
		Session lion = signup("lion");

		postItem(lion, spec.path(), spec.body().apply(0))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PROFILE_NOT_COMPLETED"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("sections")
	void 토큰이_없으면_401이다(Spec spec) throws Exception {
		mockMvc.perform(post(spec.path()).contentType(MediaType.APPLICATION_JSON)
						.content(json(spec.body().apply(0))))
				.andExpect(status().isUnauthorized());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("sections")
	void 승인된_회원도_쓸_수_있다(Spec spec) throws Exception {
		Session lion = onboarded(STUDENT);
		setStatus(lion.userId(), "APPROVED");

		postItem(lion, spec.path(), spec.body().apply(0)).andExpect(status().isCreated());
	}
}
