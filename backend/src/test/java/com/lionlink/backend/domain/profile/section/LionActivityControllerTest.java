package com.lionlink.backend.domain.profile.section;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** PROFILE-11 멋사 활동. 가입 기수(13기) 항목은 온보딩이 isDefault로 만든다. */
class LionActivityControllerTest extends ProfileSectionApiTest {

	private static final String PATH = "/api/v1/me/lion-activities";

	@Test
	void 멋사_활동을_추가하면_201과_항목이_내려오고_프로필에_반영된다() throws Exception {
		Session lion = onboarded(STUDENT);

		postItem(lion, PATH, "{'generation':12,'role':'STAFF','parts':['DESIGN','BACKEND']}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.generation").value(12))
				.andExpect(jsonPath("$.role").value("STAFF"))
				.andExpect(jsonPath("$.parts", contains("DESIGN", "BACKEND")))
				.andExpect(jsonPath("$.isDefault").value(false));

		myProfile(lion)
				.andExpect(jsonPath("$.lionActivities", hasSize(2)))
				.andExpect(jsonPath("$.lionActivities[0].generation").value(13))
				.andExpect(jsonPath("$.lionActivities[1].generation").value(12));
	}

	static Stream<Arguments> 잘못된_멋사_활동_요청() {
		return Stream.of(
				Arguments.of("기수 누락", "{'role':'LION','parts':['BACKEND']}", "generation"),
				Arguments.of("기수 0", "{'generation':0,'role':'LION','parts':['BACKEND']}", "generation"),
				Arguments.of("기수 15", "{'generation':15,'role':'LION','parts':['BACKEND']}", "generation"),
				Arguments.of("역할 누락", "{'generation':12,'parts':['BACKEND']}", "role"),
				Arguments.of("파트 없음", "{'generation':12,'role':'LION','parts':[]}", "parts"),
				Arguments.of("파트 3개", "{'generation':12,'role':'LION','parts':['BACKEND','FRONTEND','DESIGN']}", "parts"),
				Arguments.of("파트 중복", "{'generation':12,'role':'LION','parts':['BACKEND','BACKEND']}", "parts"),
				Arguments.of("없는 역할", "{'generation':12,'role':'LEADER','parts':['BACKEND']}", null));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("잘못된_멋사_활동_요청")
	void 잘못된_요청은_400이다(String name, String body, String field) throws Exception {
		Session lion = onboarded(STUDENT);

		var result = postItem(lion, PATH, body)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		if (field != null) {
			result.andExpect(jsonPath("$.fieldErrors[*].field", hasItem(field)));
		}
	}

	@Test
	void 같은_기수를_또_추가하면_409다() throws Exception {
		Session lion = onboarded(STUDENT);
		create(lion, PATH, "{'generation':12,'role':'LION','parts':['BACKEND']}");

		postItem(lion, PATH, "{'generation':12,'role':'STAFF','parts':['DESIGN']}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE_ITEM"));
		postItem(lion, PATH, "{'generation':13,'role':'LION','parts':['BACKEND']}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE_ITEM"));
	}

	@Test
	void 수정할_때_자기_기수는_중복으로_보지_않고_다른_항목의_기수는_409다() throws Exception {
		Session lion = onboarded(STUDENT);
		long id = create(lion, PATH, "{'generation':12,'role':'LION','parts':['BACKEND']}");
		create(lion, PATH, "{'generation':11,'role':'LION','parts':['BACKEND']}");

		putItem(lion, PATH, id, "{'generation':12,'role':'PRESIDENT','parts':['FRONTEND']}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("PRESIDENT"))
				.andExpect(jsonPath("$.parts", contains("FRONTEND")));

		putItem(lion, PATH, id, "{'generation':11,'role':'LION','parts':['BACKEND']}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE_ITEM"));
		putItem(lion, PATH, id, "{'generation':10,'role':'PART_LEAD','parts':['BACKEND']}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.generation").value(10));
	}

	@Test
	void 가입_기수_항목은_역할과_파트만_바뀐다() throws Exception {
		Session lion = onboarded(STUDENT);
		List<Number> ids = fromProfile(lion, "$.lionActivities[?(@.isDefault==true)].id");
		long defaultId = ids.get(0).longValue();

		putItem(lion, PATH, defaultId, "{'generation':13,'role':'STAFF','parts':['DESIGN','BACKEND']}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.generation").value(13))
				.andExpect(jsonPath("$.role").value("STAFF"))
				.andExpect(jsonPath("$.parts", contains("DESIGN", "BACKEND")))
				.andExpect(jsonPath("$.isDefault").value(true));
	}

	@Test
	void 가입_기수_항목의_기수를_바꾸려_하면_400이고_그대로다() throws Exception {
		Session lion = onboarded(STUDENT);
		List<Number> ids = fromProfile(lion, "$.lionActivities[?(@.isDefault==true)].id");
		long defaultId = ids.get(0).longValue();

		putItem(lion, PATH, defaultId, "{'generation':12,'role':'LION','parts':['BACKEND']}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		myProfile(lion).andExpect(jsonPath("$.lionActivities[0].generation").value(13));
	}

	@Test
	void 가입_기수_항목은_지울_수_없고_일반_항목은_지워진다() throws Exception {
		Session lion = onboarded(STUDENT);
		List<Number> ids = fromProfile(lion, "$.lionActivities[?(@.isDefault==true)].id");
		long id = create(lion, PATH, "{'generation':12,'role':'LION','parts':['BACKEND']}");

		deleteItem(lion, PATH, ids.get(0).longValue())
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("DEFAULT_ITEM_NOT_DELETABLE"));
		deleteItem(lion, PATH, id).andExpect(status().isNoContent());

		myProfile(lion).andExpect(jsonPath("$.lionActivities", hasSize(1)));
	}
}
