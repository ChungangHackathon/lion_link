package com.lionlink.backend.domain.admin.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionlink.backend.domain.notification.entity.Notification;
import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.section.lionactivity.LionActivity;
import com.lionlink.backend.domain.profile.section.lionactivity.LionRole;
import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.security.DevHeaderCurrentUserProvider;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminSignupReviewControllerTest {

	private static final String USER_HEADER = DevHeaderCurrentUserProvider.HEADER;
	private static final String SIGNUPS = "/api/v1/admin/signups";
	private static final AtomicInteger SEQUENCE = new AtomicInteger();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private UserRepository userRepository;

	private Long admin;

	@BeforeEach
	void setUp() {
		admin = signup("운영진", 12, UserStatus.APPROVED);
		column(admin, "role", "ADMIN");
	}

	// ---- ADMIN-03 승인 ----

	@Test
	void 승인하면_상태가_바뀌고_처리자와_시각이_기록되고_본인에게_알림이_간다() throws Exception {
		Long target = signup("김사자", 13, UserStatus.PENDING);

		mockMvc.perform(post(SIGNUPS + "/" + target + "/approve").header(USER_HEADER, admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(target))
				.andExpect(jsonPath("$.name").value("김사자"))
				.andExpect(jsonPath("$.status").value("APPROVED"))
				.andExpect(jsonPath("$.reviewedAt").isNotEmpty());

		User user = reload(target);
		assertEquals(UserStatus.APPROVED, user.getStatus());
		assertEquals(admin, user.getReviewedBy());
		assertNotNull(user.getReviewedAt());

		List<Notification> notifications = notificationsOf(target);
		assertEquals(1, notifications.size());
		assertEquals("SIGNUP_APPROVED", notifications.get(0).getType());
		assertEquals("ACCOUNT", notifications.get(0).getTab().name());
	}

	@Test
	void 반려된_회원도_승인할_수_있고_반려_사유가_비워진다() throws Exception {
		Long target = signup("반려사자", 13, UserStatus.REJECTED);
		column(target, "reject_reason", "이름·기수가 맞지 않아요");

		mockMvc.perform(post(SIGNUPS + "/" + target + "/approve").header(USER_HEADER, admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APPROVED"));

		User user = reload(target);
		assertEquals(UserStatus.APPROVED, user.getStatus());
		assertNull(user.getRejectReason());
	}

	@Test
	void 이미_승인된_회원을_승인하면_409_ALREADY_PROCESSED이고_알림은_없다() throws Exception {
		Long target = signup("승인사자", 13, UserStatus.APPROVED);

		mockMvc.perform(post(SIGNUPS + "/" + target + "/approve").header(USER_HEADER, admin))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ALREADY_PROCESSED"));

		assertEquals(0, notificationsOf(target).size());
	}

	@Test
	void 없는_회원이나_탈퇴한_회원을_승인하면_404다() throws Exception {
		Long withdrawn = signup("탈퇴사자", 13, UserStatus.PENDING);
		TestUsers.withdraw(entityManager, withdrawn);

		mockMvc.perform(post(SIGNUPS + "/999999/approve").header(USER_HEADER, admin))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
		mockMvc.perform(post(SIGNUPS + "/" + withdrawn + "/approve").header(USER_HEADER, admin))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void 관리자가_아니면_403이고_로그인하지_않으면_401이다() throws Exception {
		Long member = signup("일반사자", 13, UserStatus.APPROVED);
		Long target = signup("대기사자", 13, UserStatus.PENDING);

		mockMvc.perform(post(SIGNUPS + "/" + target + "/approve").header(USER_HEADER, member))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		mockMvc.perform(post(SIGNUPS + "/" + target + "/approve"))
				.andExpect(status().isUnauthorized());

		assertEquals(UserStatus.PENDING, reload(target).getStatus());
	}

	// ---- 동시 처리 ----

	@Test
	void 다른_관리자가_먼저_반려한_회원을_다시_반려하면_조건부_갱신이_막아_409다() throws Exception {
		Long target = signup("동시사자", 13, UserStatus.PENDING);
		// 이 관리자가 목록에서 PENDING으로 읽어 둔 상태를 재현한다(영속성 컨텍스트에 PENDING 엔티티가 남아 있음).
		assertEquals(UserStatus.PENDING, entityManager.find(User.class, target).getStatus());
		// 그 사이 다른 관리자가 먼저 반려했다. 영속성 컨텍스트는 이 변경을 모른다.
		entityManager.createNativeQuery("update users set status = 'REJECTED', reject_reason = '먼저 처리' where id = :id")
				.setParameter("id", target)
				.executeUpdate();

		// 엔티티 상태만 보고 판단하면 아직 PENDING이라 통과해 버린다. 조건부 UPDATE는 DB 상태를 보므로 0행이 되어 409다.
		mockMvc.perform(post(SIGNUPS + "/" + target + "/reject")
						.header(USER_HEADER, admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"reasonType\":\"MISMATCH\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ALREADY_PROCESSED"));
	}

	@Test
	void 승인_갱신은_한_번만_반영되고_두_번째는_영향받은_행이_0이다() {
		Long target = signup("연속사자", 13, UserStatus.PENDING);
		Long other = signup("다른관리자", 12, UserStatus.APPROVED);
		LocalDateTime now = LocalDateTime.now();

		int first = userRepository.approve(target, admin, now);
		int second = userRepository.approve(target, other, now);

		assertEquals(1, first);
		assertEquals(0, second);
		assertEquals(admin, reload(target).getReviewedBy());
	}

	@Test
	void 이미_승인된_회원을_다른_관리자가_뒤늦게_반려하면_409다() throws Exception {
		Long target = signup("승인먼저", 13, UserStatus.PENDING);
		assertEquals(UserStatus.PENDING, entityManager.find(User.class, target).getStatus());
		entityManager.createNativeQuery("update users set status = 'APPROVED' where id = :id")
				.setParameter("id", target)
				.executeUpdate();

		mockMvc.perform(post(SIGNUPS + "/" + target + "/reject")
						.header(USER_HEADER, admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"reasonType\":\"UNVERIFIED\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ALREADY_PROCESSED"));
	}

	// ---- ADMIN-04 반려 ----

	@Test
	void 고정_사유로_반려하면_고정_문구가_저장되고_알림은_없다() throws Exception {
		Long target = signup("반려대상", 13, UserStatus.PENDING);

		mockMvc.perform(post(SIGNUPS + "/" + target + "/reject")
						.header(USER_HEADER, admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"reasonType\":\"MISMATCH\",\"reasonText\":\"무시되는 글\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(target))
				.andExpect(jsonPath("$.status").value("REJECTED"))
				.andExpect(jsonPath("$.rejectReason").value("이름·기수가 맞지 않아요"));

		User user = reload(target);
		assertEquals(UserStatus.REJECTED, user.getStatus());
		assertEquals(admin, user.getReviewedBy());
		assertNotNull(user.getReviewedAt());
		assertEquals(0, notificationsOf(target).size());
	}

	@Test
	void UNVERIFIED_사유는_고정_문구로_저장된다() throws Exception {
		Long target = signup("확인불가", 13, UserStatus.PENDING);

		mockMvc.perform(post(SIGNUPS + "/" + target + "/reject")
						.header(USER_HEADER, admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"reasonType\":\"UNVERIFIED\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rejectReason").value("동문인지 확인할 수 없어요"));
	}

	@Test
	void 기타_사유는_입력한_문구가_저장된다() throws Exception {
		Long target = signup("기타대상", 13, UserStatus.PENDING);

		mockMvc.perform(post(SIGNUPS + "/" + target + "/reject")
						.header(USER_HEADER, admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"reasonType\":\"ETC\",\"reasonText\":\"  중복 가입이 의심돼요  \"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rejectReason").value("중복 가입이 의심돼요"));
	}

	@Test
	void 기타_사유가_5자_미만이거나_100자_초과이거나_없으면_400이다() throws Exception {
		Long target = signup("검증대상", 13, UserStatus.PENDING);
		String tooLong = "가".repeat(101);

		for (String body : List.of(
				"{\"reasonType\":\"ETC\",\"reasonText\":\"짧아요\"}",
				"{\"reasonType\":\"ETC\",\"reasonText\":\"" + tooLong + "\"}",
				"{\"reasonType\":\"ETC\"}",
				"{\"reasonType\":\"NOPE\"}",
				"{}")) {
			mockMvc.perform(post(SIGNUPS + "/" + target + "/reject")
							.header(USER_HEADER, admin)
							.contentType(MediaType.APPLICATION_JSON)
							.content(body))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		}
		assertEquals(UserStatus.PENDING, reload(target).getStatus());
	}

	@Test
	void 승인됨_또는_반려됨_회원을_반려하면_409다() throws Exception {
		Long approved = signup("승인됨", 13, UserStatus.APPROVED);
		Long rejected = signup("반려됨", 13, UserStatus.REJECTED);

		for (Long target : List.of(approved, rejected)) {
			mockMvc.perform(post(SIGNUPS + "/" + target + "/reject")
							.header(USER_HEADER, admin)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"reasonType\":\"UNVERIFIED\"}"))
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.code").value("ALREADY_PROCESSED"));
		}
	}

	@Test
	void 없는_회원을_반려하면_404이고_일반_회원은_403이다() throws Exception {
		Long member = signup("일반사자", 13, UserStatus.APPROVED);
		Long target = signup("대기사자", 13, UserStatus.PENDING);
		String body = "{\"reasonType\":\"UNVERIFIED\"}";

		mockMvc.perform(post(SIGNUPS + "/999999/reject")
						.header(USER_HEADER, admin).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isNotFound());
		mockMvc.perform(post(SIGNUPS + "/" + target + "/reject")
						.header(USER_HEADER, member).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isForbidden());
	}

	// ---- ADMIN-05 선택 승인 ----

	@Test
	void 선택_승인은_성공과_실패를_나눠_돌려주고_성공한_사람만_승인한다() throws Exception {
		Long pending = signup("대기하나", 13, UserStatus.PENDING);
		Long rejected = signup("반려하나", 13, UserStatus.REJECTED);
		Long approved = signup("승인하나", 13, UserStatus.APPROVED);

		mockMvc.perform(post(SIGNUPS + "/approve-batch")
						.header(USER_HEADER, admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"userIds\":[" + pending + "," + rejected + "," + approved + ",999999]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.succeeded.length()").value(2))
				.andExpect(jsonPath("$.succeeded[0].userId").value(pending))
				.andExpect(jsonPath("$.succeeded[0].name").value("대기하나"))
				.andExpect(jsonPath("$.succeeded[0].status").value("APPROVED"))
				.andExpect(jsonPath("$.succeeded[1].userId").value(rejected))
				.andExpect(jsonPath("$.failed.length()").value(2))
				.andExpect(jsonPath("$.failed[0].userId").value(approved))
				.andExpect(jsonPath("$.failed[0].code").value("ALREADY_PROCESSED"))
				.andExpect(jsonPath("$.failed[1].userId").value(999999))
				.andExpect(jsonPath("$.failed[1].code").value("NOT_FOUND"));

		assertEquals(UserStatus.APPROVED, reload(pending).getStatus());
		assertEquals(UserStatus.APPROVED, reload(rejected).getStatus());
		assertEquals(admin, reload(pending).getReviewedBy());
		assertEquals(1, notificationsOf(pending).size());
		assertEquals(1, notificationsOf(rejected).size());
		assertEquals(0, notificationsOf(approved).size());
	}

	@Test
	void 선택_승인에_비었거나_50개_초과이거나_중복이면_400이다() throws Exception {
		Long target = signup("배치대상", 13, UserStatus.PENDING);
		List<String> ids = new ArrayList<>();
		LongStream.rangeClosed(1, 51).forEach(id -> ids.add(String.valueOf(id)));

		for (String body : List.of(
				"{\"userIds\":[]}",
				"{}",
				"{\"userIds\":[" + String.join(",", ids) + "]}",
				"{\"userIds\":[" + target + "," + target + "]}")) {
			mockMvc.perform(post(SIGNUPS + "/approve-batch")
							.header(USER_HEADER, admin)
							.contentType(MediaType.APPLICATION_JSON)
							.content(body))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		}
		assertEquals(UserStatus.PENDING, reload(target).getStatus());
	}

	@Test
	void 일반_회원은_선택_승인을_못한다() throws Exception {
		Long member = signup("일반사자", 13, UserStatus.APPROVED);

		mockMvc.perform(post(SIGNUPS + "/approve-batch")
						.header(USER_HEADER, member)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"userIds\":[1]}"))
				.andExpect(status().isForbidden());
	}

	// ---- ADMIN-06 이름·기수 수정 ----

	@Test
	void 승인된_회원의_이름과_기수를_고치면_자동_생성_멋사_활동_기수도_바뀐다() throws Exception {
		Long target = signup("오타사자", 13, UserStatus.APPROVED);
		Long activityId = saveLionActivity(target, 13, true);

		mockMvc.perform(patch(SIGNUPS + "/" + target + "/signup-info")
						.header(USER_HEADER, admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"  김사자  \",\"generation\":12}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(target))
				.andExpect(jsonPath("$.name").value("김사자"))
				.andExpect(jsonPath("$.generation").value(12));

		User user = reload(target);
		assertEquals("김사자", user.getName());
		assertEquals(12, user.getGeneration());
		assertEquals(UserStatus.APPROVED, user.getStatus());
		assertEquals(12, entityManager.find(LionActivity.class, activityId).getGeneration());
	}

	@Test
	void 반려된_회원의_이름도_고칠_수_있다() throws Exception {
		Long target = signup("반려오타", 13, UserStatus.REJECTED);

		mockMvc.perform(patch(SIGNUPS + "/" + target + "/signup-info")
						.header(USER_HEADER, admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"고친이름\",\"generation\":13}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("고친이름"));
		assertEquals(UserStatus.REJECTED, reload(target).getStatus());
	}

	@Test
	void 바뀔_기수에_이미_다른_멋사_활동이_있으면_409_DUPLICATE_ITEM이고_아무것도_바뀌지_않는다() throws Exception {
		Long target = signup("중복사자", 13, UserStatus.APPROVED);
		Long defaultId = saveLionActivity(target, 13, true);
		saveLionActivity(target, 12, false);

		mockMvc.perform(patch(SIGNUPS + "/" + target + "/signup-info")
						.header(USER_HEADER, admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"중복사자\",\"generation\":12}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE_ITEM"));

		assertEquals(13, reload(target).getGeneration());
		assertEquals(13, entityManager.find(LionActivity.class, defaultId).getGeneration());
	}

	@Test
	void 이름_길이와_기수_범위가_틀리면_400이다() throws Exception {
		Long target = signup("검증사자", 13, UserStatus.PENDING);

		for (String body : List.of(
				"{\"name\":\"김\",\"generation\":13}",
				"{\"name\":\"" + "가".repeat(21) + "\",\"generation\":13}",
				"{\"name\":\"김사자\",\"generation\":0}",
				"{\"name\":\"김사자\",\"generation\":15}",
				"{\"name\":\"김사자\"}",
				"{\"generation\":13}")) {
			mockMvc.perform(patch(SIGNUPS + "/" + target + "/signup-info")
							.header(USER_HEADER, admin)
							.contentType(MediaType.APPLICATION_JSON)
							.content(body))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		}
	}

	@Test
	void 없는_회원은_404이고_일반_회원은_403이다() throws Exception {
		Long member = signup("일반사자", 13, UserStatus.APPROVED);
		Long withdrawn = signup("탈퇴사자", 13, UserStatus.APPROVED);
		TestUsers.withdraw(entityManager, withdrawn);
		String body = "{\"name\":\"김사자\",\"generation\":13}";

		mockMvc.perform(patch(SIGNUPS + "/999999/signup-info")
						.header(USER_HEADER, admin).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isNotFound());
		mockMvc.perform(patch(SIGNUPS + "/" + withdrawn + "/signup-info")
						.header(USER_HEADER, admin).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isNotFound());
		mockMvc.perform(patch(SIGNUPS + "/" + member + "/signup-info")
						.header(USER_HEADER, member).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isForbidden());
	}

	// ---- helper ----

	private Long signup(String name, int generation, UserStatus status) {
		User user = User.builder()
				.email("review" + SEQUENCE.incrementAndGet() + "@test.com")
				.googleName("Google " + name)
				.name(name)
				.generation(generation)
				.build();
		entityManager.persist(user);
		entityManager.flush();
		entityManager.clear();
		column(user.getId(), "status", status.name());
		return user.getId();
	}

	private Long saveLionActivity(Long userId, int generation, boolean isDefault) {
		LionActivity activity = LionActivity.builder()
				.userId(userId)
				.generation(generation)
				.role(LionRole.LION)
				.part1(LionPart.BACKEND)
				.isDefault(isDefault)
				.build();
		entityManager.persist(activity);
		entityManager.flush();
		entityManager.clear();
		return activity.getId();
	}

	private User reload(Long userId) {
		entityManager.flush();
		entityManager.clear();
		return entityManager.find(User.class, userId);
	}

	private List<Notification> notificationsOf(Long userId) {
		entityManager.flush();
		return entityManager.createQuery("select n from Notification n where n.userId = :id", Notification.class)
				.setParameter("id", userId)
				.getResultList();
	}

	private void column(Long userId, String column, Object value) {
		entityManager.flush();
		entityManager.createNativeQuery("update users set " + column + " = :value where id = :id")
				.setParameter("value", value)
				.setParameter("id", userId)
				.executeUpdate();
		entityManager.clear();
	}
}
