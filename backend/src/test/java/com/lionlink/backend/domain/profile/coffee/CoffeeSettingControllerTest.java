package com.lionlink.backend.domain.profile.coffee;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.lionlink.backend.domain.coffeechat.entity.ReceiveMode;
import com.lionlink.backend.global.config.JacksonConfig;
import com.lionlink.backend.support.AuthIntegrationTest;
import com.lionlink.backend.support.TestProfiles;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** PROFILE-12~15 커피챗 받기 설정·홈 스위치·기간 */
class CoffeeSettingControllerTest extends AuthIntegrationTest {

	private static final String SETTINGS = "/api/v1/me/coffee-settings";
	private static final String SWITCH = "/api/v1/me/coffee-settings/switch";
	private static final String PERIODS = "/api/v1/me/coffee-periods";
	private static final String STUDENT = "{'lionParts':['BACKEND'],'status':'STUDENT','interests':['BACKEND']}";

	private static final String VALID_PUT = """
			{'defaultMode':'ON','acceptGroup':true,'topics':['CAREER'],'modes':['ONLINE'],
			 'availableSlots':[],'preferredArea':null}""";

	private final LocalDate today = LocalDate.now(JacksonConfig.ZONE);

	// ---------- PROFILE-12 조회 ----------

	@Test
	void 가입_직후_기본_설정이_내려온다() throws Exception {
		Session lion = onboarded();

		getSettings(lion)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.defaultMode").value("ON"))
				.andExpect(jsonPath("$.acceptGroup").value(true))
				.andExpect(jsonPath("$.topics", empty()))
				.andExpect(jsonPath("$.modes", contains("ONLINE", "OFFLINE")))
				.andExpect(jsonPath("$.availableSlots", empty()))
				.andExpect(jsonPath("$.preferredArea").value(nullValue()))
				.andExpect(jsonPath("$.periodKind").value("REST"))
				.andExpect(jsonPath("$.periods", empty()))
				.andExpect(jsonPath("$.status.availability").value("AVAILABLE"))
				.andExpect(jsonPath("$.status.nextAvailableDate").value(nullValue()));
	}

	@Test
	void 쉬는_기간_안이면_RESTING이고_종료일_다음_날이_다시_받는_날이다() throws Exception {
		Session lion = onboarded();
		TestProfiles.coffeePeriod(entityManager, lion.userId(), today.minusDays(1), today.plusDays(2));

		getSettings(lion)
				.andExpect(jsonPath("$.status.availability").value("RESTING"))
				.andExpect(jsonPath("$.status.nextAvailableDate").value(today.plusDays(3).toString()))
				.andExpect(jsonPath("$.periods", hasSize(1)));
	}

	@Test
	void 지난_기간은_목록에서_빠지고_미래_쉬는_기간은_받는_중이다() throws Exception {
		Session lion = onboarded();
		TestProfiles.coffeePeriod(entityManager, lion.userId(), today.minusDays(10), today.minusDays(5));
		TestProfiles.coffeePeriod(entityManager, lion.userId(), today.plusDays(3), today.plusDays(4));

		getSettings(lion)
				.andExpect(jsonPath("$.periods", hasSize(1)))
				.andExpect(jsonPath("$.periods[0].startDate").value(today.plusDays(3).toString()))
				.andExpect(jsonPath("$.status.availability").value("AVAILABLE"));
	}

	@Test
	void 기본_상태가_OFF면_받는_기간_밖은_NOT_ACCEPTING이고_다음_시작일을_알려준다() throws Exception {
		Session lion = onboarded();
		setMode(lion, "OFF");
		getSettings(lion)
				.andExpect(jsonPath("$.periodKind").value("OPEN"))
				.andExpect(jsonPath("$.status.availability").value("NOT_ACCEPTING"))
				.andExpect(jsonPath("$.status.nextAvailableDate").value(nullValue()));

		TestProfiles.coffeePeriod(entityManager, lion.userId(), today.plusDays(5), today.plusDays(9));
		getSettings(lion)
				.andExpect(jsonPath("$.status.availability").value("NOT_ACCEPTING"))
				.andExpect(jsonPath("$.status.nextAvailableDate").value(today.plusDays(5).toString()));

		TestProfiles.coffeePeriod(entityManager, lion.userId(), today.minusDays(1), today.plusDays(1));
		getSettings(lion)
				.andExpect(jsonPath("$.status.availability").value("AVAILABLE"))
				.andExpect(jsonPath("$.periods", hasSize(2)))
				.andExpect(jsonPath("$.periods[0].startDate").value(today.minusDays(1).toString()));
	}

	@Test
	void 반려_온보딩_전_토큰_없음은_조회할_수_없다() throws Exception {
		Session rejected = onboarded();
		reject(rejected.userId(), "사유");
		getSettings(rejected).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("NOT_APPROVED"));

		Session fresh = signup("fresh");
		getSettings(fresh).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PROFILE_NOT_COMPLETED"));

		mockMvc.perform(get(SETTINGS)).andExpect(status().isUnauthorized());
	}

	// ---------- PROFILE-13 저장 ----------

	@Test
	void 설정을_통째로_저장하면_정렬되어_조회에_반영된다() throws Exception {
		Session lion = onboarded();

		putSettings(lion, """
				{'defaultMode':'ON','acceptGroup':false,'topics':['CAREER','STUDY'],'modes':['OFFLINE'],
				 'availableSlots':[{'day':'WED','times':['EVENING','MORNING']},{'day':'MON','times':['AFTERNOON']}],
				 'preferredArea':'  강남  '}""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.acceptGroup").value(false))
				.andExpect(jsonPath("$.topics", contains("CAREER", "STUDY")))
				.andExpect(jsonPath("$.modes", contains("OFFLINE")))
				.andExpect(jsonPath("$.availableSlots[0].day").value("MON"))
				.andExpect(jsonPath("$.availableSlots[1].day").value("WED"))
				.andExpect(jsonPath("$.availableSlots[1].times", contains("MORNING", "EVENING")))
				.andExpect(jsonPath("$.preferredArea").value("강남"));

		getSettings(lion)
				.andExpect(jsonPath("$.topics", contains("CAREER", "STUDY")))
				.andExpect(jsonPath("$.availableSlots", hasSize(2)))
				.andExpect(jsonPath("$.preferredArea").value("강남"));
	}

	@Test
	void 주제와_가능_시간을_비워서_저장할_수_있다() throws Exception {
		Session lion = onboarded();

		putSettings(lion, "{'defaultMode':'ON','acceptGroup':true,'modes':['ONLINE','OFFLINE'],'preferredArea':''}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.topics", empty()))
				.andExpect(jsonPath("$.availableSlots", empty()))
				.andExpect(jsonPath("$.preferredArea").value(nullValue()));
	}

	static Stream<Arguments> 잘못된_설정_요청() {
		return Stream.of(
				Arguments.of("방식이 비어 있음", "{'defaultMode':'ON','acceptGroup':true,'modes':[]}", "modes"),
				Arguments.of("방식 누락", "{'defaultMode':'ON','acceptGroup':true}", "modes"),
				Arguments.of("기본 상태 누락", "{'acceptGroup':true,'modes':['ONLINE']}", "defaultMode"),
				Arguments.of("여러 명 여부 누락", "{'defaultMode':'ON','modes':['ONLINE']}", "acceptGroup"),
				Arguments.of("없는 주제", "{'defaultMode':'ON','acceptGroup':true,'modes':['ONLINE'],'topics':['GAME']}", null),
				Arguments.of("주제 중복", "{'defaultMode':'ON','acceptGroup':true,'modes':['ONLINE'],'topics':['CAREER','CAREER']}", "topics"),
				Arguments.of("없는 방식", "{'defaultMode':'ON','acceptGroup':true,'modes':['PHONE']}", null),
				Arguments.of("선호 지역 31자", "{'defaultMode':'ON','acceptGroup':true,'modes':['ONLINE'],'preferredArea':'" + "가".repeat(31) + "'}", "preferredArea"),
				Arguments.of("요일 중복", "{'defaultMode':'ON','acceptGroup':true,'modes':['ONLINE'],'availableSlots':[{'day':'MON','times':['MORNING']},{'day':'MON','times':['EVENING']}]}", null),
				Arguments.of("없는 요일", "{'defaultMode':'ON','acceptGroup':true,'modes':['ONLINE'],'availableSlots':[{'day':'FUNDAY','times':['MORNING']}]}", null),
				Arguments.of("시간대 비어 있음", "{'defaultMode':'ON','acceptGroup':true,'modes':['ONLINE'],'availableSlots':[{'day':'MON','times':[]}]}", null));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("잘못된_설정_요청")
	void 잘못된_설정은_400이다(String name, String body, String field) throws Exception {
		Session lion = onboarded();

		var result = putSettings(lion, body)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		if (field != null) {
			result.andExpect(jsonPath("$.fieldErrors[*].field", hasItem(field)));
		}
		getSettings(lion).andExpect(jsonPath("$.modes", contains("ONLINE", "OFFLINE")));
	}

	@Test
	void 기본_상태를_바꾸는데_기간이_있으면_확인_없이는_409고_확인하면_기간이_지워진다() throws Exception {
		Session lion = onboarded();
		TestProfiles.coffeePeriod(entityManager, lion.userId(), today.plusDays(3), today.plusDays(5));

		putSettings(lion, VALID_PUT.replace("'ON'", "'OFF'"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("PERIODS_WILL_BE_CLEARED"));
		getSettings(lion)
				.andExpect(jsonPath("$.defaultMode").value("ON"))
				.andExpect(jsonPath("$.periods", hasSize(1)));

		putSettings(lion, VALID_PUT.replace("'ON'", "'OFF'").replace("'preferredArea'", "'x'")
				.replace("}", ",'confirmClearPeriods':true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.defaultMode").value("OFF"))
				.andExpect(jsonPath("$.periodKind").value("OPEN"))
				.andExpect(jsonPath("$.periods", empty()));
	}

	@Test
	void 기본_상태를_안_바꾸면_기간이_있어도_확인이_필요_없다() throws Exception {
		Session lion = onboarded();
		TestProfiles.coffeePeriod(entityManager, lion.userId(), today.plusDays(3), today.plusDays(5));

		putSettings(lion, VALID_PUT).andExpect(status().isOk()).andExpect(jsonPath("$.periods", hasSize(1)));
	}

	@Test
	void 기간이_없으면_확인_없이_기본_상태를_바꾼다() throws Exception {
		Session lion = onboarded();

		putSettings(lion, VALID_PUT.replace("'ON'", "'OFF'"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.defaultMode").value("OFF"));
	}

	@Test
	void 지난_기간만_있으면_확인_없이_바꾸고_지난_기간도_지워진다() throws Exception {
		Session lion = onboarded();
		TestProfiles.coffeePeriod(entityManager, lion.userId(), today.minusDays(10), today.minusDays(5));

		putSettings(lion, VALID_PUT.replace("'ON'", "'OFF'")).andExpect(status().isOk());

		Number count = (Number) entityManager.createNativeQuery("select count(*) from coffee_periods where user_id = :id")
				.setParameter("id", lion.userId()).getSingleResult();
		org.junit.jupiter.api.Assertions.assertEquals(0, count.intValue());
	}

	// ---------- PROFILE-15 기간 추가·삭제 ----------

	@Test
	void 기간을_추가하면_201이고_조회에_나온다() throws Exception {
		Session lion = onboarded();

		postPeriod(lion, today.plusDays(3), today.plusDays(10), false)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.startDate").value(today.plusDays(3).toString()))
				.andExpect(jsonPath("$.endDate").value(today.plusDays(10).toString()));

		getSettings(lion).andExpect(jsonPath("$.periods", hasSize(1)));
	}

	@Test
	void 오늘부터_시작하고_시작일에서_180일_뒤까지는_된다() throws Exception {
		Session lion = onboarded();

		postPeriod(lion, today, today.plusDays(180), false).andExpect(status().isCreated());
	}

	@Test
	void 기간_추가가_잘못되면_400이다() throws Exception {
		Session lion = onboarded();

		postPeriod(lion, today.plusDays(5), today.plusDays(4), false)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.message").value("종료일이 시작일보다 빨라요."));
		postPeriod(lion, today.minusDays(1), today.plusDays(1), false).andExpect(status().isBadRequest());
		postPeriod(lion, today.plusYears(1).plusDays(1), today.plusYears(1).plusDays(3), false)
				.andExpect(status().isBadRequest());
		postPeriod(lion, today, today.plusDays(181), false).andExpect(status().isBadRequest());
		mockMvc.perform(post(PERIODS).header(HttpHeaders.AUTHORIZATION, bearer(lion))
						.contentType(MediaType.APPLICATION_JSON).content(json("{'startDate':'" + today + "'}")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[*].field", hasItem("endDate")));
		getSettings(lion).andExpect(jsonPath("$.periods", empty()));
	}

	@Test
	void 겹치는_기간은_409고_합치기를_켜면_하나로_합쳐진다() throws Exception {
		Session lion = onboarded();
		postPeriod(lion, today.plusDays(3), today.plusDays(10), false).andExpect(status().isCreated());
		postPeriod(lion, today.plusDays(20), today.plusDays(25), false).andExpect(status().isCreated());

		postPeriod(lion, today.plusDays(8), today.plusDays(12), false)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("PERIOD_OVERLAP"));

		postPeriod(lion, today.plusDays(8), today.plusDays(22), true)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.startDate").value(today.plusDays(3).toString()))
				.andExpect(jsonPath("$.endDate").value(today.plusDays(25).toString()));
		getSettings(lion).andExpect(jsonPath("$.periods", hasSize(1)));
	}

	@Test
	void 기간은_최대_5개고_지난_기간은_세지_않는다() throws Exception {
		Session lion = onboarded();
		TestProfiles.coffeePeriod(entityManager, lion.userId(), today.minusDays(10), today.minusDays(5));
		for (int i = 0; i < 5; i++) {
			postPeriod(lion, today.plusDays(10L * i + 1), today.plusDays(10L * i + 3), false)
					.andExpect(status().isCreated());
		}

		postPeriod(lion, today.plusDays(100), today.plusDays(101), false)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("PERIOD_LIMIT"));
	}

	@Test
	void 기간을_삭제하면_204고_남의_기간과_없는_기간은_404다() throws Exception {
		Session lion = onboarded();
		Session other = onboarded("other");
		long id = periodId(postPeriod(lion, today.plusDays(3), today.plusDays(5), false));

		deletePeriod(other, id).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
		deletePeriod(lion, 999999).andExpect(status().isNotFound());
		getSettings(lion).andExpect(jsonPath("$.periods", hasSize(1)));

		deletePeriod(lion, id).andExpect(status().isNoContent());
		getSettings(lion).andExpect(jsonPath("$.periods", empty()));
	}

	@Test
	void 기간_API도_반려_온보딩_전_토큰_없음은_막힌다() throws Exception {
		Session rejected = onboarded();
		reject(rejected.userId(), "사유");
		postPeriod(rejected, today.plusDays(1), today.plusDays(2), false)
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("NOT_APPROVED"));

		Session fresh = signup("fresh");
		postPeriod(fresh, today.plusDays(1), today.plusDays(2), false)
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PROFILE_NOT_COMPLETED"));

		mockMvc.perform(delete(PERIODS + "/1")).andExpect(status().isUnauthorized());
	}

	// ---------- PROFILE-14 홈 스위치 ----------

	@Test
	void 받는_중에_끄면_오늘부터_쉬는_기간이_만들어진다() throws Exception {
		Session lion = onboarded();

		switchTo(lion, "{'on':false,'restFor':'WEEK_1'}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.periods", hasSize(1)))
				.andExpect(jsonPath("$.periods[0].startDate").value(today.toString()))
				.andExpect(jsonPath("$.periods[0].endDate").value(today.plusDays(6).toString()))
				.andExpect(jsonPath("$.status.availability").value("RESTING"))
				.andExpect(jsonPath("$.status.nextAvailableDate").value(today.plusDays(7).toString()));
	}

	@Test
	void 쉬는_기간_종류별_종료일이_다르다() throws Exception {
		Session week2 = onboarded("week2");
		switchTo(week2, "{'on':false,'restFor':'WEEK_2'}")
				.andExpect(jsonPath("$.periods[0].endDate").value(today.plusDays(13).toString()));

		Session month = onboarded("month");
		switchTo(month, "{'on':false,'restFor':'MONTH_1'}")
				.andExpect(jsonPath("$.periods[0].endDate").value(today.plusMonths(1).minusDays(1).toString()));

		Session custom = onboarded("custom");
		switchTo(custom, "{'on':false,'restFor':'CUSTOM','startDate':'" + today.plusDays(2) + "','endDate':'"
				+ today.plusDays(5) + "'}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.periods[0].startDate").value(today.plusDays(2).toString()))
				.andExpect(jsonPath("$.status.availability").value("AVAILABLE"));
	}

	@Test
	void 끌_때_종류가_없거나_직접_입력이_잘못되면_400이다() throws Exception {
		Session lion = onboarded();

		switchTo(lion, "{'on':false}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		switchTo(lion, "{'on':false,'restFor':'CUSTOM'}").andExpect(status().isBadRequest());
		switchTo(lion, "{'on':false,'restFor':'CUSTOM','startDate':'" + today.plusDays(5) + "','endDate':'"
				+ today.plusDays(1) + "'}").andExpect(status().isBadRequest());
		switchTo(lion, "{'restFor':'WEEK_1'}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[*].field", hasItem("on")));
		switchTo(lion, "{'on':false,'restFor':'NEVER'}").andExpect(status().isBadRequest());
		getSettings(lion).andExpect(jsonPath("$.periods", empty()));
	}

	@Test
	void 쉬는_중에_켜면_진행_중인_기간이_어제까지로_줄어든다() throws Exception {
		Session lion = onboarded();
		TestProfiles.coffeePeriod(entityManager, lion.userId(), today.minusDays(2), today.plusDays(4));

		switchTo(lion, "{'on':true}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.periods", empty()))
				.andExpect(jsonPath("$.status.availability").value("AVAILABLE"));
		// 어제까지로 줄어든 기간은 지난 기간이라 목록에서는 빠지고 DB에는 남는다.
		org.junit.jupiter.api.Assertions.assertEquals(today.minusDays(1), onlyPeriodEnd(lion));
	}

	@Test
	void 오늘_시작한_쉬는_기간은_켜면_지워진다() throws Exception {
		Session lion = onboarded();
		switchTo(lion, "{'on':false,'restFor':'WEEK_1'}").andExpect(status().isOk());

		switchTo(lion, "{'on':true}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.periods", empty()))
				.andExpect(jsonPath("$.status.availability").value("AVAILABLE"));
	}

	@Test
	void 바로_이어지는_쉬는_기간은_함께_끝난다() throws Exception {
		Session lion = onboarded();
		TestProfiles.coffeePeriod(entityManager, lion.userId(), today.minusDays(1), today.plusDays(1));
		TestProfiles.coffeePeriod(entityManager, lion.userId(), today.plusDays(2), today.plusDays(4));
		TestProfiles.coffeePeriod(entityManager, lion.userId(), today.plusDays(10), today.plusDays(12));

		switchTo(lion, "{'on':true}")
				.andExpect(jsonPath("$.periods", hasSize(1)))
				.andExpect(jsonPath("$.periods[0].startDate").value(today.plusDays(10).toString()))
				.andExpect(jsonPath("$.status.availability").value("AVAILABLE"));
		// 어제까지로 줄어든 첫 기간 + 남은 먼 기간 = 2개, 바로 이어지던 둘째 기간은 지워졌다.
		Number count = (Number) entityManager.createNativeQuery("select count(*) from coffee_periods where user_id = :id")
				.setParameter("id", lion.userId()).getSingleResult();
		org.junit.jupiter.api.Assertions.assertEquals(2, count.intValue());
	}

	@Test
	void 기본_상태가_OFF일_때_받는_기간_안에서_끄면_기간이_줄어든다() throws Exception {
		Session lion = onboarded();
		setMode(lion, "OFF");
		TestProfiles.coffeePeriod(entityManager, lion.userId(), today.minusDays(3), today.plusDays(3));

		switchTo(lion, "{'on':false}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.periods", empty()))
				.andExpect(jsonPath("$.status.availability").value("NOT_ACCEPTING"));
		org.junit.jupiter.api.Assertions.assertEquals(today.minusDays(1), onlyPeriodEnd(lion));
	}

	@Test
	void 안_받는_중에_켜면_기본_상태가_ON이_되고_기간이_있으면_확인이_필요하다() throws Exception {
		Session lion = onboarded();
		setMode(lion, "OFF");
		TestProfiles.coffeePeriod(entityManager, lion.userId(), today.plusDays(5), today.plusDays(7));

		switchTo(lion, "{'on':true}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("PERIODS_WILL_BE_CLEARED"));
		getSettings(lion).andExpect(jsonPath("$.defaultMode").value("OFF"));

		switchTo(lion, "{'on':true,'confirmClearPeriods':true}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.defaultMode").value("ON"))
				.andExpect(jsonPath("$.periods", empty()))
				.andExpect(jsonPath("$.status.availability").value("AVAILABLE"));
	}

	@Test
	void 스위치는_기간이_없으면_확인_없이_기본_상태를_바꾼다() throws Exception {
		Session lion = onboarded();
		setMode(lion, "OFF");

		switchTo(lion, "{'on':true}").andExpect(status().isOk()).andExpect(jsonPath("$.defaultMode").value("ON"));
	}

	@Test
	void 다시_켤_때까지_쉬기는_기본_상태를_OFF로_바꾼다() throws Exception {
		Session lion = onboarded();
		TestProfiles.coffeePeriod(entityManager, lion.userId(), today.plusDays(5), today.plusDays(7));

		switchTo(lion, "{'on':false,'restFor':'UNTIL_RESUME'}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("PERIODS_WILL_BE_CLEARED"));

		switchTo(lion, "{'on':false,'restFor':'UNTIL_RESUME','confirmClearPeriods':true}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.defaultMode").value("OFF"))
				.andExpect(jsonPath("$.periods", empty()))
				.andExpect(jsonPath("$.status.availability").value("NOT_ACCEPTING"));
	}

	@Test
	void 이미_원하는_상태면_아무것도_바꾸지_않고_200이다() throws Exception {
		Session receiving = onboarded("receiving");
		switchTo(receiving, "{'on':true}").andExpect(status().isOk()).andExpect(jsonPath("$.periods", empty()));

		Session resting = onboarded("resting");
		TestProfiles.coffeePeriod(entityManager, resting.userId(), today.minusDays(1), today.plusDays(2));
		switchTo(resting, "{'on':false,'restFor':'WEEK_1'}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.periods", hasSize(1)))
				.andExpect(jsonPath("$.periods[0].endDate").value(today.plusDays(2).toString()));

		Session off = onboarded("off");
		setMode(off, "OFF");
		switchTo(off, "{'on':false}").andExpect(status().isOk()).andExpect(jsonPath("$.defaultMode").value("OFF"));
	}

	@Test
	void 연달아_눌러도_결과가_같다() throws Exception {
		Session lion = onboarded();

		switchTo(lion, "{'on':false,'restFor':'WEEK_1'}").andExpect(status().isOk());
		switchTo(lion, "{'on':false,'restFor':'WEEK_1'}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.periods", hasSize(1)));
		switchTo(lion, "{'on':true}").andExpect(status().isOk());
		switchTo(lion, "{'on':true}").andExpect(status().isOk()).andExpect(jsonPath("$.periods", empty()));
	}

	@Test
	void 기간이_5개면_기간_종류는_409고_다시_켤_때까지_쉬기만_받는다() throws Exception {
		Session lion = onboarded();
		for (int i = 0; i < 5; i++) {
			// 한 주 쉬기(오늘~6일 뒤)와 겹치지 않게 멀리 둔다.
			postPeriod(lion, today.plusDays(20L + 10L * i), today.plusDays(22L + 10L * i), false)
					.andExpect(status().isCreated());
		}

		switchTo(lion, "{'on':false,'restFor':'WEEK_1'}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("PERIOD_LIMIT"));
		switchTo(lion, "{'on':false,'restFor':'UNTIL_RESUME','confirmClearPeriods':true}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.defaultMode").value("OFF"));
	}

	@Test
	void 겹치는_쉬는_기간은_스위치에서_합쳐진다() throws Exception {
		Session lion = onboarded();
		postPeriod(lion, today.plusDays(3), today.plusDays(20), false).andExpect(status().isCreated());

		switchTo(lion, "{'on':false,'restFor':'WEEK_2'}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.periods", hasSize(1)))
				.andExpect(jsonPath("$.periods[0].startDate").value(today.toString()))
				.andExpect(jsonPath("$.periods[0].endDate").value(today.plusDays(20).toString()));
	}

	@Test
	void 스위치도_반려_온보딩_전_토큰_없음은_막힌다() throws Exception {
		Session rejected = onboarded();
		reject(rejected.userId(), "사유");
		switchTo(rejected, "{'on':true}").andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("NOT_APPROVED"));

		Session fresh = signup("fresh");
		switchTo(fresh, "{'on':true}").andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PROFILE_NOT_COMPLETED"));

		mockMvc.perform(post(SWITCH).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 승인된_회원도_쓸_수_있다() throws Exception {
		Session lion = onboarded();
		setStatus(lion.userId(), "APPROVED");

		getSettings(lion).andExpect(status().isOk());
		switchTo(lion, "{'on':false,'restFor':'WEEK_1'}").andExpect(status().isOk());
	}

	// ---------- 도구 ----------

	private Session onboarded() throws Exception {
		return onboarded("lion");
	}

	private Session onboarded(String sub) throws Exception {
		Session session = signup(sub);
		onboard(session, STUDENT).andExpect(status().isOk());
		return session;
	}

	/** 기본 상태를 직접 바꾼다 (기간이 없을 때 PUT으로 바꾸는 것과 같다). */
	private void setMode(Session session, String mode) throws Exception {
		ReceiveMode.valueOf(mode);
		putSettings(session, VALID_PUT.replace("'ON'", "'" + mode + "'")).andExpect(status().isOk());
	}

	private ResultActions getSettings(Session session) throws Exception {
		return mockMvc.perform(get(SETTINGS).header(HttpHeaders.AUTHORIZATION, bearer(session)));
	}

	private ResultActions putSettings(Session session, String body) throws Exception {
		return mockMvc.perform(put(SETTINGS).header(HttpHeaders.AUTHORIZATION, bearer(session))
				.contentType(MediaType.APPLICATION_JSON).content(json(body)));
	}

	private ResultActions switchTo(Session session, String body) throws Exception {
		return mockMvc.perform(post(SWITCH).header(HttpHeaders.AUTHORIZATION, bearer(session))
				.contentType(MediaType.APPLICATION_JSON).content(json(body)));
	}

	private ResultActions postPeriod(Session session, LocalDate start, LocalDate end, boolean merge) throws Exception {
		return mockMvc.perform(post(PERIODS).header(HttpHeaders.AUTHORIZATION, bearer(session))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("{'startDate':'" + start + "','endDate':'" + end + "','mergeOverlap':" + merge + "}")));
	}

	private ResultActions deletePeriod(Session session, long id) throws Exception {
		return mockMvc.perform(delete(PERIODS + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(session)));
	}

	/** 목록에서 빠진 지난 기간까지 포함해 DB에 있는 유일한 기간의 종료일. */
	private LocalDate onlyPeriodEnd(Session session) {
		entityManager.flush();
		Object end = entityManager.createNativeQuery("select end_date from coffee_periods where user_id = :id")
				.setParameter("id", session.userId()).getSingleResult();
		return LocalDate.parse(end.toString());
	}

	private long periodId(ResultActions result) throws Exception {
		String body = result.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}
}
