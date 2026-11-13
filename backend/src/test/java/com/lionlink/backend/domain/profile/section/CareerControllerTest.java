package com.lionlink.backend.domain.profile.section;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** PROFILE-11 경력 */
class CareerControllerTest extends ProfileSectionApiTest {

	private static final String PATH = "/api/v1/me/careers";

	private static final String VALID = """
			{'company':'핀테크A','jobTitle':'백엔드 개발자','employmentType':'FULL_TIME',
			 'startYm':'2024-03','endYm':null,'isCurrent':true,'description':'결제 API 개발'}""";

	@Test
	void 경력을_추가하면_201과_항목이_내려오고_프로필에_반영된다() throws Exception {
		Session lion = onboarded(STUDENT);

		postItem(lion, PATH, VALID)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.company").value("핀테크A"))
				.andExpect(jsonPath("$.jobTitle").value("백엔드 개발자"))
				.andExpect(jsonPath("$.employmentType").value("FULL_TIME"))
				.andExpect(jsonPath("$.startYm").value("2024-03"))
				.andExpect(jsonPath("$.endYm").value(nullValue()))
				.andExpect(jsonPath("$.isCurrent").value(true))
				.andExpect(jsonPath("$.description").value("결제 API 개발"));

		myProfile(lion)
				.andExpect(jsonPath("$.careers", hasSize(1)))
				.andExpect(jsonPath("$.careers[0].company").value("핀테크A"));
	}

	@Test
	void 종료_년월이_있는_지난_경력도_추가된다() throws Exception {
		Session lion = onboarded(STUDENT);

		postItem(lion, PATH, """
				{'company':'  스타트업B  ','jobTitle':'인턴','employmentType':'INTERN',
				 'startYm':'2022-01','endYm':'2022-06','description':'  '}""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.company").value("스타트업B"))
				.andExpect(jsonPath("$.endYm").value("2022-06"))
				.andExpect(jsonPath("$.isCurrent").value(false))
				.andExpect(jsonPath("$.description").value(nullValue()));
	}

	static Stream<Arguments> 잘못된_경력_요청() {
		return Stream.of(
				Arguments.of("회사명 누락", "{'jobTitle':'a','employmentType':'ETC','startYm':'2024-01','isCurrent':true}", "company"),
				Arguments.of("고용 형태 누락", "{'company':'a','jobTitle':'a','startYm':'2024-01','isCurrent':true}", "employmentType"),
				Arguments.of("시작 년월 누락", "{'company':'a','jobTitle':'a','employmentType':'ETC','isCurrent':true}", "startYm"),
				Arguments.of("회사명 31자", "{'company':'" + "가".repeat(31) + "','jobTitle':'a','employmentType':'ETC','startYm':'2024-01','isCurrent':true}", "company"),
				Arguments.of("설명 301자", "{'company':'a','jobTitle':'a','employmentType':'ETC','startYm':'2024-01','isCurrent':true,'description':'" + "가".repeat(301) + "'}", "description"),
				Arguments.of("종료가 시작보다 빠름", "{'company':'a','jobTitle':'a','employmentType':'ETC','startYm':'2024-05','endYm':'2024-04'}", "endYm"),
				Arguments.of("진행 중인데 종료 입력", "{'company':'a','jobTitle':'a','employmentType':'ETC','startYm':'2024-05','endYm':'2024-06','isCurrent':true}", "endYm"),
				Arguments.of("종료도 진행 중도 없음", "{'company':'a','jobTitle':'a','employmentType':'ETC','startYm':'2024-05'}", "endYm"),
				Arguments.of("시작이 미래", "{'company':'a','jobTitle':'a','employmentType':'ETC','startYm':'2999-01','isCurrent':true}", "startYm"),
				Arguments.of("없는 고용 형태", "{'company':'a','jobTitle':'a','employmentType':'PART_TIME','startYm':'2024-01','isCurrent':true}", null));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("잘못된_경력_요청")
	void 잘못된_요청은_400이다(String name, String body, String field) throws Exception {
		Session lion = onboarded(STUDENT);

		var result = postItem(lion, PATH, body)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		if (field != null) {
			result.andExpect(jsonPath("$.fieldErrors[*].field", hasItem(field)));
		}
		myProfile(lion).andExpect(jsonPath("$.careers", hasSize(0)));
	}

	@Test
	void 경력을_수정하면_통째로_바뀐다() throws Exception {
		Session lion = onboarded(STUDENT);
		long id = create(lion, PATH, VALID);

		putItem(lion, PATH, id, """
				{'company':'대기업C','jobTitle':'시니어','employmentType':'CONTRACT',
				 'startYm':'2020-01','endYm':'2023-12','isCurrent':false}""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.company").value("대기업C"))
				.andExpect(jsonPath("$.employmentType").value("CONTRACT"))
				.andExpect(jsonPath("$.endYm").value("2023-12"))
				.andExpect(jsonPath("$.isCurrent").value(false))
				.andExpect(jsonPath("$.description").value(nullValue()));
	}

	@Test
	void 진행_중으로_바꾸면_종료_년월이_비워진다() throws Exception {
		Session lion = onboarded(STUDENT);
		long id = create(lion, PATH, """
				{'company':'a','jobTitle':'b','employmentType':'ETC','startYm':'2022-01','endYm':'2022-12'}""");

		putItem(lion, PATH, id, VALID)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.endYm").value(nullValue()))
				.andExpect(jsonPath("$.isCurrent").value(true));
	}

	@Test
	void 온보딩이_만든_경력은_시작_년월과_고용_형태를_채워야_수정된다() throws Exception {
		Session lion = onboarded(EMPLOYED);
		Number id = fromProfile(lion, "$.careers[0].id");

		putItem(lion, PATH, id.longValue(), "{'company':'핀테크A','jobTitle':'백엔드 개발자','isCurrent':true}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[*].field", hasItem("startYm")))
				.andExpect(jsonPath("$.fieldErrors[*].field", hasItem("employmentType")));

		putItem(lion, PATH, id.longValue(), VALID)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.employmentType").value("FULL_TIME"))
				.andExpect(jsonPath("$.startYm").value("2024-03"));
	}

	@Test
	void 경력을_삭제하면_204이고_프로필에서_사라진다() throws Exception {
		Session lion = onboarded(STUDENT);
		long id = create(lion, PATH, VALID);

		deleteItem(lion, PATH, id).andExpect(status().isNoContent());

		myProfile(lion).andExpect(jsonPath("$.careers", hasSize(0)));
	}

	@Test
	void 프로필_조회는_진행_중인_경력이_맨_위다() throws Exception {
		Session lion = onboarded(STUDENT);
		create(lion, PATH, "{'company':'지난','jobTitle':'a','employmentType':'ETC','startYm':'2023-01','endYm':'2023-12'}");
		create(lion, PATH, VALID);
		create(lion, PATH, "{'company':'더지난','jobTitle':'a','employmentType':'ETC','startYm':'2020-01','endYm':'2020-12'}");

		List<String> companies = fromProfile(lion, "$.careers[*].company");

		org.junit.jupiter.api.Assertions.assertEquals(List.of("핀테크A", "지난", "더지난"), companies);
	}
}
