package com.lionlink.backend.domain.profile.basic.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionlink.backend.domain.coffeechat.entity.CoffeeSetting;
import com.lionlink.backend.domain.coffeechat.entity.MeetingMode;
import com.lionlink.backend.domain.coffeechat.entity.ReceiveMode;
import com.lionlink.backend.domain.coffeechat.repository.CoffeeSettingRepository;
import com.lionlink.backend.domain.notification.entity.NotificationSetting;
import com.lionlink.backend.domain.notification.repository.NotificationSettingRepository;
import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.ProfileStatus;
import com.lionlink.backend.domain.profile.basic.entity.Profile;
import com.lionlink.backend.domain.profile.basic.entity.ProfileLink;
import com.lionlink.backend.domain.profile.basic.entity.Visibility;
import com.lionlink.backend.domain.profile.basic.repository.ProfileRepository;
import com.lionlink.backend.domain.profile.section.career.Career;
import com.lionlink.backend.domain.profile.section.career.CareerRepository;
import com.lionlink.backend.domain.profile.section.education.Education;
import com.lionlink.backend.domain.profile.section.education.EducationRepository;
import com.lionlink.backend.domain.profile.section.lionactivity.LionActivity;
import com.lionlink.backend.domain.profile.section.lionactivity.LionActivityRepository;
import com.lionlink.backend.domain.profile.section.lionactivity.LionRole;
import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.support.AuthIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

class ProfileOnboardingControllerTest extends AuthIntegrationTest {

	private static final String EMPLOYED_BODY = """
			{'lionParts':['FRONTEND','BACKEND'],'status':'EMPLOYED','interests':['BACKEND','DATA_AI'],
			 'company':'핀테크A','jobTitle':'백엔드 개발자','link':'https://github.com/lion','headline':'핀테크 백엔드 개발자'}""";
	private static final String STUDENT_BODY = "{'lionParts':['BACKEND'],'status':'STUDENT','interests':['BACKEND']}";

	@Autowired
	private ProfileRepository profileRepository;
	@Autowired
	private CareerRepository careerRepository;
	@Autowired
	private EducationRepository educationRepository;
	@Autowired
	private LionActivityRepository lionActivityRepository;
	@Autowired
	private CoffeeSettingRepository coffeeSettingRepository;
	@Autowired
	private NotificationSettingRepository notificationSettingRepository;

	// ---------- 정상 ----------

	@Test
	void 재직_회원이_온보딩하면_프로필과_자동_등록_항목이_모두_만들어진다() throws Exception {
		Session lion = signup("lion");

		onboard(lion, EMPLOYED_BODY)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(lion.userId()))
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.profileCompleted").value(true));
		entityManager.flush();
		entityManager.clear();

		Profile profile = profileRepository.findById(lion.userId()).orElseThrow();
		assertEquals(List.of(LionPart.FRONTEND, LionPart.BACKEND), profile.getLionParts());
		assertEquals(ProfileStatus.EMPLOYED, profile.getStatus());
		assertEquals(List.of("BACKEND", "DATA_AI"), profile.getInterests());
		assertEquals("핀테크 백엔드 개발자", profile.getHeadline());
		assertEquals("lion@gmail.com", profile.getContactEmail());
		assertEquals(List.of(new ProfileLink("GITHUB", "https://github.com/lion")), profile.getLinks());
		assertEquals(Visibility.CONNECTED, profile.getEmailVisibility());
		assertEquals(Visibility.CONNECTED, profile.getPhoneVisibility());
		assertEquals(Visibility.PUBLIC, profile.getLinksVisibility());
		assertEquals(Visibility.PUBLIC, profile.getCompanyVisibility());
		assertTrue(entityManager.find(User.class, lion.userId()).isProfileCompleted());

		List<Career> careers = careerRepository.findByUserIdOrderByIsCurrentDescStartYmDescIdDesc(lion.userId());
		assertEquals(1, careers.size());
		assertEquals("핀테크A", careers.get(0).getCompany());
		assertEquals("백엔드 개발자", careers.get(0).getTitle());
		assertTrue(careers.get(0).isCurrent());
		assertNull(careers.get(0).getEmploymentType());
		assertNull(careers.get(0).getStartYm());

		List<Education> educations = educationRepository.findByUserIdOrderByIsCurrentDescStartYmDescIdDesc(lion.userId());
		assertEquals(1, educations.size());
		assertEquals("중앙대학교", educations.get(0).getSchool());

		List<LionActivity> activities = lionActivityRepository.findByUserIdOrderByGenerationDesc(lion.userId());
		assertEquals(1, activities.size());
		assertEquals(13, activities.get(0).getGeneration());
		assertEquals(LionRole.LION, activities.get(0).getRole());
		assertEquals(LionPart.FRONTEND, activities.get(0).getPart1());
		assertEquals(LionPart.BACKEND, activities.get(0).getPart2());
		assertTrue(activities.get(0).isDefault());

		CoffeeSetting coffee = coffeeSettingRepository.findById(lion.userId()).orElseThrow();
		assertEquals(ReceiveMode.ON, coffee.getDefaultMode());
		assertEquals(List.of(MeetingMode.ONLINE, MeetingMode.OFFLINE), coffee.getModes());

		NotificationSetting notification = notificationSettingRepository.findById(lion.userId()).orElseThrow();
		assertTrue(notification.isEmailCoffeeChat());
		assertTrue(notification.isEmailMessageRequest());
	}

	@Test
	void 재직이_아닌_회원은_경력과_링크_없이_만들어진다() throws Exception {
		Session lion = signup("lion");

		onboard(lion, STUDENT_BODY).andExpect(status().isOk());
		entityManager.flush();
		entityManager.clear();

		assertTrue(careerRepository.findByUserIdOrderByIsCurrentDescStartYmDescIdDesc(lion.userId()).isEmpty());
		assertEquals(1, educationRepository.findByUserIdOrderByIsCurrentDescStartYmDescIdDesc(lion.userId()).size());
		assertNull(profileRepository.findById(lion.userId()).orElseThrow().getLinks());
		LionActivity activity = lionActivityRepository.findByUserIdAndIsDefaultTrue(lion.userId()).orElseThrow();
		assertEquals(LionPart.BACKEND, activity.getPart1());
		assertNull(activity.getPart2());
	}

	@Test
	void 운영진_기수에_가입_기수가_있으면_멋사_활동이_STAFF로_바뀌고_나머지_기수가_추가된다() throws Exception {
		Session lion = signup("lion", "김사자", 13);

		onboard(lion, """
				{'lionParts':['BACKEND'],'status':'STUDENT','interests':['BACKEND'],
				 'staffActivities':[{'generation':13,'parts':['PLANNING']},{'generation':12,'parts':['DESIGN','FRONTEND']}]}""")
				.andExpect(status().isOk());
		entityManager.flush();
		entityManager.clear();

		List<LionActivity> activities = lionActivityRepository.findByUserIdOrderByGenerationDesc(lion.userId());
		assertEquals(2, activities.size());

		LionActivity joined = activities.get(0);
		assertEquals(13, joined.getGeneration());
		assertEquals(LionRole.STAFF, joined.getRole());
		assertEquals(LionPart.BACKEND, joined.getPart1());
		assertTrue(joined.isDefault());

		LionActivity extra = activities.get(1);
		assertEquals(12, extra.getGeneration());
		assertEquals(LionRole.STAFF, extra.getRole());
		assertEquals(LionPart.DESIGN, extra.getPart1());
		assertEquals(LionPart.FRONTEND, extra.getPart2());
		assertFalse(extra.isDefault());
	}

	@Test
	void 운영진_기수에_가입_기수가_없으면_기본_멋사_활동은_LION으로_남는다() throws Exception {
		Session lion = signup("lion", "김사자", 13);

		onboard(lion, """
				{'lionParts':['BACKEND'],'status':'STUDENT','interests':['BACKEND'],
				 'staffActivities':[{'generation':12,'parts':['DESIGN']}]}""")
				.andExpect(status().isOk());
		entityManager.flush();
		entityManager.clear();

		assertEquals(LionRole.LION, lionActivityRepository.findByUserIdAndIsDefaultTrue(lion.userId()).orElseThrow().getRole());
		assertEquals(2, lionActivityRepository.findByUserIdOrderByGenerationDesc(lion.userId()).size());
	}

	@Test
	void 링크_종류는_주소를_보고_자동_지정된다() throws Exception {
		Session lion = signup("lion");

		onboard(lion, "{'lionParts':['BACKEND'],'status':'STUDENT','interests':['BACKEND'],"
				+ "'link':'https://www.linkedin.com/in/lion'}").andExpect(status().isOk());
		entityManager.flush();
		entityManager.clear();

		assertEquals("LINKEDIN", profileRepository.findById(lion.userId()).orElseThrow().getLinks().get(0).type());
	}

	// ---------- 검증 실패 400 ----------

	@Test
	void 멋사_파트가_3개면_400() throws Exception {
		Session lion = signup("lion");

		onboard(lion, "{'lionParts':['PLANNING','DESIGN','BACKEND'],'status':'STUDENT','interests':['BACKEND']}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void 멋사_파트가_중복이면_400() throws Exception {
		Session lion = signup("lion");

		onboard(lion, "{'lionParts':['BACKEND','BACKEND'],'status':'STUDENT','interests':['BACKEND']}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void 운영진_기수가_중복이면_400() throws Exception {
		Session lion = signup("lion");

		onboard(lion, """
				{'lionParts':['BACKEND'],'status':'STUDENT','interests':['BACKEND'],
				 'staffActivities':[{'generation':12,'parts':['DESIGN']},{'generation':12,'parts':['PLANNING']}]}""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void 운영진_기수가_범위를_벗어나거나_파트가_없으면_400() throws Exception {
		Session lion = signup("lion");

		onboard(lion, """
				{'lionParts':['BACKEND'],'status':'STUDENT','interests':['BACKEND'],
				 'staffActivities':[{'generation':15,'parts':['DESIGN']}]}""")
				.andExpect(status().isBadRequest());
		onboard(lion, """
				{'lionParts':['BACKEND'],'status':'STUDENT','interests':['BACKEND'],
				 'staffActivities':[{'generation':12,'parts':[]}]}""")
				.andExpect(status().isBadRequest());
	}

	@Test
	void 링크가_URL_형식이_아니면_400() throws Exception {
		Session lion = signup("lion");

		onboard(lion, "{'lionParts':['BACKEND'],'status':'STUDENT','interests':['BACKEND'],'link':'github.com/lion'}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("link"));
	}

	@Test
	void 재직인데_회사명이나_직무가_없으면_400() throws Exception {
		Session lion = signup("lion");

		onboard(lion, "{'lionParts':['BACKEND'],'status':'EMPLOYED','interests':['BACKEND'],'company':'핀테크A'}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void 필수값이_없거나_열거값이_틀리거나_관심분야가_4개면_400() throws Exception {
		Session lion = signup("lion");

		onboard(lion, "{'status':'STUDENT','interests':['BACKEND']}").andExpect(status().isBadRequest());
		onboard(lion, "{'lionParts':['BACKEND'],'interests':['BACKEND']}").andExpect(status().isBadRequest());
		onboard(lion, "{'lionParts':['ETC'],'status':'STUDENT','interests':['BACKEND']}").andExpect(status().isBadRequest());
		onboard(lion, "{'lionParts':['BACKEND'],'status':'STUDENT','interests':[]}").andExpect(status().isBadRequest());
		onboard(lion, "{'lionParts':['BACKEND'],'status':'STUDENT','interests':['BACKEND','DESIGN','APP','ETC']}")
				.andExpect(status().isBadRequest());
	}

	@Test
	void 검증에_실패하면_아무것도_만들어지지_않는다() throws Exception {
		Session lion = signup("lion");

		onboard(lion, "{'lionParts':['BACKEND'],'status':'STUDENT'}").andExpect(status().isBadRequest());
		entityManager.flush();
		entityManager.clear();

		assertFalse(profileRepository.existsById(lion.userId()));
		assertFalse(entityManager.find(User.class, lion.userId()).isProfileCompleted());
	}

	// ---------- 409 ----------

	@Test
	void 두_번째_호출은_409_ALREADY_COMPLETED() throws Exception {
		Session lion = signup("lion");
		onboard(lion, STUDENT_BODY).andExpect(status().isOk());

		onboard(lion, EMPLOYED_BODY)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ALREADY_COMPLETED"));
		entityManager.flush();
		entityManager.clear();

		assertEquals(1, lionActivityRepository.findByUserIdOrderByGenerationDesc(lion.userId()).size());
		assertTrue(careerRepository.findByUserIdOrderByIsCurrentDescStartYmDescIdDesc(lion.userId()).isEmpty());
	}

	// ---------- 접근 제어 ----------

	@Test
	void 승인_회원도_온보딩할_수_있다() throws Exception {
		Session lion = signup("lion");
		setStatus(lion.userId(), "APPROVED");

		onboard(lion, STUDENT_BODY)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APPROVED"))
				.andExpect(jsonPath("$.profileCompleted").value(true));
	}

	@Test
	void 반려_회원은_온보딩도_조회도_403_NOT_APPROVED() throws Exception {
		Session lion = signup("lion");
		reject(lion.userId(), "기수 정보를 확인할 수 없어요.");

		onboard(lion, STUDENT_BODY)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("NOT_APPROVED"));
		mockMvc.perform(get("/api/v1/me/profile").header(HttpHeaders.AUTHORIZATION, bearer(lion)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("NOT_APPROVED"));
		entityManager.flush();
		entityManager.clear();

		assertFalse(profileRepository.existsById(lion.userId()));
	}

	@Test
	void 어노테이션이_없는_기존_API는_승인_대기_회원이_여전히_403() throws Exception {
		Session lion = signup("lion");

		mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, bearer(lion)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("NOT_APPROVED"));
	}

	@Test
	void 토큰이_없으면_온보딩은_401() throws Exception {
		mockMvc.perform(post("/api/v1/me/profile/onboarding")
						.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
						.content(json(STUDENT_BODY)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}
}
