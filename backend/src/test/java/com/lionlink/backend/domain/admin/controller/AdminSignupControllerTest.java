package com.lionlink.backend.domain.admin.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.global.security.DevHeaderCurrentUserProvider;
import com.lionlink.backend.support.TestProfiles;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminSignupControllerTest {

	private static final String USER_HEADER = DevHeaderCurrentUserProvider.HEADER;
	private static final String SIGNUPS = "/api/v1/admin/signups";
	private static final String PENDING_COUNT = "/api/v1/admin/signups/pending-count";
	private static final AtomicInteger SEQUENCE = new AtomicInteger();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EntityManager entityManager;

	private Long admin;

	@BeforeEach
	void setUp() {
		admin = signup("운영진", 12, UserStatus.APPROVED);
		column(admin, "role", "ADMIN");
	}

	@Test
	void 기본_탭은_승인_대기이고_가입이_오래된_순으로_나온다() throws Exception {
		Long newer = signup("나중사자", 13, UserStatus.PENDING);
		Long older = signup("먼저사자", 13, UserStatus.PENDING);
		column(newer, "created_at", LocalDateTime.of(2026, 11, 2, 10, 0));
		column(older, "created_at", LocalDateTime.of(2026, 11, 1, 10, 0));
		signup("승인된사자", 13, UserStatus.APPROVED);

		mockMvc.perform(get(SIGNUPS).header(USER_HEADER, admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(2)))
				.andExpect(jsonPath("$.items[0].userId").value(older))
				.andExpect(jsonPath("$.items[1].userId").value(newer))
				.andExpect(jsonPath("$.items[0].name").value("먼저사자"))
				.andExpect(jsonPath("$.items[0].generation").value(13))
				.andExpect(jsonPath("$.items[0].googleName").value("Google 먼저사자"))
				.andExpect(jsonPath("$.items[0].signedUpAt").value(notNullValue()))
				.andExpect(jsonPath("$.items[0].reviewedAt").value(nullValue()))
				.andExpect(jsonPath("$.items[0].reviewedBy").value(nullValue()))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(20))
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.hasNext").value(false));
	}

	@Test
	void 승인됨_탭은_처리_시각이_최신인_순이고_처리_정보가_없는_계정은_맨_뒤다() throws Exception {
		Long early = signup("일찍승인", 13, UserStatus.APPROVED);
		Long late = signup("늦게승인", 13, UserStatus.APPROVED);
		review(early, admin, LocalDateTime.of(2026, 11, 1, 9, 0));
		review(late, admin, LocalDateTime.of(2026, 11, 3, 9, 0));
		signup("대기사자", 13, UserStatus.PENDING);

		mockMvc.perform(get(SIGNUPS).param("status", "APPROVED").header(USER_HEADER, admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(3)))
				.andExpect(jsonPath("$.items[0].userId").value(late))
				.andExpect(jsonPath("$.items[0].reviewedBy").value("운영진"))
				.andExpect(jsonPath("$.items[0].reviewedAt").value(notNullValue()))
				.andExpect(jsonPath("$.items[1].userId").value(early))
				// 관리자 계정은 승인 절차 없이 APPROVED라 처리 정보가 없다
				.andExpect(jsonPath("$.items[2].userId").value(admin))
				.andExpect(jsonPath("$.items[2].reviewedAt").value(nullValue()))
				.andExpect(jsonPath("$.items[2].reviewedBy").value(nullValue()));
	}

	@Test
	void 반려됨_탭은_반려된_회원만_처리_시각_최신순으로_사유와_함께_나온다() throws Exception {
		Long first = signup("먼저반려", 13, UserStatus.REJECTED);
		Long second = signup("나중반려", 13, UserStatus.REJECTED);
		review(first, admin, LocalDateTime.of(2026, 11, 1, 9, 0));
		review(second, admin, LocalDateTime.of(2026, 11, 2, 9, 0));
		column(first, "reject_reason", "이름·기수가 맞지 않아요");
		signup("대기사자", 13, UserStatus.PENDING);

		mockMvc.perform(get(SIGNUPS).param("status", "REJECTED").header(USER_HEADER, admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(2)))
				.andExpect(jsonPath("$.items[0].userId").value(second))
				.andExpect(jsonPath("$.items[0].rejectReason").value(nullValue()))
				.andExpect(jsonPath("$.items[1].userId").value(first))
				.andExpect(jsonPath("$.items[1].rejectReason").value("이름·기수가 맞지 않아요"))
				.andExpect(jsonPath("$.items[1].reviewedBy").value("운영진"));
	}

	@Test
	void 탈퇴한_회원은_어느_탭에도_나오지_않고_건수에도_세지_않는다() throws Exception {
		Long pending = signup("탈퇴대기", 13, UserStatus.PENDING);
		Long approved = signup("탈퇴승인", 13, UserStatus.APPROVED);
		Long rejected = signup("탈퇴반려", 13, UserStatus.REJECTED);
		signup("남은대기", 13, UserStatus.PENDING);
		TestUsers.withdraw(entityManager, pending);
		TestUsers.withdraw(entityManager, approved);
		TestUsers.withdraw(entityManager, rejected);

		mockMvc.perform(get(SIGNUPS).header(USER_HEADER, admin))
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].name").value("남은대기"));
		mockMvc.perform(get(SIGNUPS).param("status", "APPROVED").header(USER_HEADER, admin))
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].userId").value(admin));
		mockMvc.perform(get(SIGNUPS).param("status", "REJECTED").header(USER_HEADER, admin))
				.andExpect(jsonPath("$.items", hasSize(0)));
		mockMvc.perform(get(PENDING_COUNT).header(USER_HEADER, admin))
				.andExpect(jsonPath("$.pending").value(1));
	}

	@Test
	void 재신청한_회원은_재신청_표시와_이전_반려_사유가_나오고_처리_정보는_숨긴다() throws Exception {
		Long reapplied = signup("재신청사자", 13, UserStatus.PENDING);
		column(reapplied, "created_at", LocalDateTime.of(2026, 11, 1, 10, 0));
		column(reapplied, "reapplied_at", LocalDateTime.of(2026, 11, 8, 9, 10));
		column(reapplied, "reject_reason", "이름·기수가 맞지 않아요");
		review(reapplied, admin, LocalDateTime.of(2026, 11, 7, 18, 0));
		Long fresh = signup("새사자", 13, UserStatus.PENDING);
		column(fresh, "created_at", LocalDateTime.of(2026, 11, 2, 10, 0));

		mockMvc.perform(get(SIGNUPS).header(USER_HEADER, admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(2)))
				.andExpect(jsonPath("$.items[0].userId").value(reapplied))
				.andExpect(jsonPath("$.items[0].reapplied").value(true))
				.andExpect(jsonPath("$.items[0].reapplyAt").value(notNullValue()))
				.andExpect(jsonPath("$.items[0].rejectReason").value("이름·기수가 맞지 않아요"))
				.andExpect(jsonPath("$.items[0].reviewedAt").value(nullValue()))
				.andExpect(jsonPath("$.items[0].reviewedBy").value(nullValue()))
				.andExpect(jsonPath("$.items[1].userId").value(fresh))
				.andExpect(jsonPath("$.items[1].reapplied").value(false))
				.andExpect(jsonPath("$.items[1].reapplyAt").value(nullValue()))
				.andExpect(jsonPath("$.items[1].rejectReason").value(nullValue()));
	}

	@Test
	void 이미_승인된_회원과_이름과_기수가_모두_같을_때만_같은_이름_기수로_표시한다() throws Exception {
		signup("김사자", 13, UserStatus.APPROVED);
		Long same = signup("김사자", 13, UserStatus.PENDING);
		Long otherGeneration = signup("김사자", 12, UserStatus.PENDING);
		Long otherName = signup("이사자", 13, UserStatus.PENDING);

		mockMvc.perform(get(SIGNUPS).header(USER_HEADER, admin))
				.andExpect(jsonPath("$.items", hasSize(3)))
				.andExpect(jsonPath("$.items[?(@.userId == " + same + ")].sameNameGeneration").value(true))
				.andExpect(jsonPath("$.items[?(@.userId == " + otherGeneration + ")].sameNameGeneration").value(false))
				.andExpect(jsonPath("$.items[?(@.userId == " + otherName + ")].sameNameGeneration").value(false));
	}

	@Test
	void 승인됨_탭에서는_자기_자신을_같은_이름_기수로_치지_않는다() throws Exception {
		Long alone = signup("혼자사자", 13, UserStatus.APPROVED);
		signup("쌍둥이", 13, UserStatus.APPROVED);
		signup("쌍둥이", 13, UserStatus.APPROVED);

		mockMvc.perform(get(SIGNUPS).param("status", "APPROVED").header(USER_HEADER, admin))
				.andExpect(jsonPath("$.items[?(@.userId == " + alone + ")].sameNameGeneration").value(false))
				.andExpect(jsonPath("$.items[?(@.name == '쌍둥이')].sameNameGeneration").value(
						org.hamcrest.Matchers.contains(true, true)));
	}

	@Test
	void 승인된_회원과_같은_이름_기수여도_반려된_회원은_비교_대상이_아니다() throws Exception {
		signup("김사자", 13, UserStatus.REJECTED);
		Long pending = signup("김사자", 13, UserStatus.PENDING);

		mockMvc.perform(get(SIGNUPS).header(USER_HEADER, admin))
				.andExpect(jsonPath("$.items[?(@.userId == " + pending + ")].sameNameGeneration").value(false));
	}

	@Test
	void 온보딩_전_회원은_파트_상태_회사가_빈_값이다() throws Exception {
		signup("온보딩전", 13, UserStatus.PENDING);

		mockMvc.perform(get(SIGNUPS).header(USER_HEADER, admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].lionParts", hasSize(0)))
				.andExpect(jsonPath("$.items[0].currentStatus").value(nullValue()))
				.andExpect(jsonPath("$.items[0].company").value(nullValue()));
	}

	@Test
	void 온보딩한_회원은_파트_현재_상태_재직_회사가_나온다_회사는_비공개여도_보인다() throws Exception {
		Long done = signup("온보딩함", 13, UserStatus.PENDING);
		TestProfiles.save(entityManager, TestProfiles.profile(done).lionPart2(LionPart.FRONTEND));
		TestProfiles.currentCareer(entityManager, done, "핀테크A", "백엔드 개발자");
		TestProfiles.visibility(entityManager, done, "company_visibility", "PRIVATE");

		mockMvc.perform(get(SIGNUPS).header(USER_HEADER, admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].lionParts[0]").value("BACKEND"))
				.andExpect(jsonPath("$.items[0].lionParts[1]").value("FRONTEND"))
				.andExpect(jsonPath("$.items[0].currentStatus").value("EMPLOYED"))
				.andExpect(jsonPath("$.items[0].company").value("핀테크A"));
	}

	@Test
	void 페이지로_나눠서_다음_페이지_여부를_알려준다() throws Exception {
		for (int i = 1; i <= 3; i++) {
			Long id = signup("대기" + i, 13, UserStatus.PENDING);
			column(id, "created_at", LocalDateTime.of(2026, 11, i, 10, 0));
		}

		mockMvc.perform(get(SIGNUPS).param("size", "2").header(USER_HEADER, admin))
				.andExpect(jsonPath("$.items", hasSize(2)))
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.hasNext").value(true));
		mockMvc.perform(get(SIGNUPS).param("size", "2").param("page", "1").header(USER_HEADER, admin))
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].name").value("대기3"))
				.andExpect(jsonPath("$.page").value(1))
				.andExpect(jsonPath("$.hasNext").value(false));
		mockMvc.perform(get(SIGNUPS).param("page", "5").header(USER_HEADER, admin))
				.andExpect(jsonPath("$.items", hasSize(0)))
				.andExpect(jsonPath("$.hasNext").value(false));
	}

	@Test
	void 일반_회원이_부르면_403_FORBIDDEN이다() throws Exception {
		Long member = signup("일반회원", 13, UserStatus.APPROVED);

		mockMvc.perform(get(SIGNUPS).header(USER_HEADER, member))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		mockMvc.perform(get(PENDING_COUNT).header(USER_HEADER, member))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void 승인_대기_회원은_관리자_검사_전에_403_NOT_APPROVED로_막힌다() throws Exception {
		Long pending = signup("대기사자", 13, UserStatus.PENDING);

		mockMvc.perform(get(SIGNUPS).header(USER_HEADER, pending))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("NOT_APPROVED"));
	}

	@Test
	void 로그인하지_않았으면_401이다() throws Exception {
		mockMvc.perform(get(SIGNUPS))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
		mockMvc.perform(get(PENDING_COUNT))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 없는_status나_범위를_벗어난_page_size는_400이다() throws Exception {
		mockMvc.perform(get(SIGNUPS).param("size", "101").header(USER_HEADER, admin))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		mockMvc.perform(get(SIGNUPS).param("size", "0").header(USER_HEADER, admin))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get(SIGNUPS).param("page", "-1").header(USER_HEADER, admin))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get(SIGNUPS).param("status", "UNKNOWN").header(USER_HEADER, admin))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		mockMvc.perform(get(SIGNUPS).param("size", "100").header(USER_HEADER, admin))
				.andExpect(status().isOk());
	}

	@Test
	void 승인_대기_건수를_센다() throws Exception {
		signup("대기1", 13, UserStatus.PENDING);
		signup("대기2", 13, UserStatus.PENDING);
		signup("승인", 13, UserStatus.APPROVED);
		signup("반려", 13, UserStatus.REJECTED);

		mockMvc.perform(get(PENDING_COUNT).header(USER_HEADER, admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pending").value(2));
	}

	@Test
	void 대기가_없으면_건수는_0이다() throws Exception {
		mockMvc.perform(get(PENDING_COUNT).header(USER_HEADER, admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pending").value(0));
	}

	/** 가입 신청한 회원을 만든다. 구글 이름은 "Google {이름}"으로 둔다. */
	private Long signup(String name, int generation, UserStatus status) {
		User user = User.builder()
				.email("signup" + SEQUENCE.incrementAndGet() + "@test.com")
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

	/** 관리자 처리(승인·반려) 결과를 재현한다. */
	private void review(Long userId, Long reviewerId, LocalDateTime reviewedAt) {
		column(userId, "reviewed_by", reviewerId);
		column(userId, "reviewed_at", reviewedAt);
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
