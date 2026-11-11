package com.lionlink.backend.domain.alumni.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionlink.backend.domain.alumni.dto.request.AlumniSearchCondition;
import com.lionlink.backend.domain.alumni.dto.response.AlumniDetailResponse;
import com.lionlink.backend.domain.alumni.dto.response.AlumniResponse;
import com.lionlink.backend.domain.alumni.exception.AlumniErrorCode;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatStatus;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeSetting;
import com.lionlink.backend.domain.coffeechat.entity.MeetingMode;
import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.ProfileStatus;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.response.PageResponse;
import com.lionlink.backend.support.TestProfiles;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AlumniServiceTest {

	private static final AlumniSearchCondition ALL = new AlumniSearchCondition(null, null, null, null, null);

	@Autowired
	private AlumniService alumniService;

	@Autowired
	private BookmarkService bookmarkService;

	@Autowired
	private EntityManager entityManager;

	private Long me;

	@BeforeEach
	void setUp() {
		me = TestUsers.approved(entityManager, "나사자");
		TestProfiles.save(entityManager, TestProfiles.profile(me));
	}

	@Test
	void 승인되고_프로필이_있는_활성_회원만_최근_가입순으로_조회한다() {
		Long kim = alumni("김사자");
		Long pending = TestUsers.create(entityManager, "대기사자", UserStatus.PENDING);
		TestProfiles.save(entityManager, TestProfiles.profile(pending));
		Long withdrawn = alumni("탈퇴사자");
		TestUsers.withdraw(entityManager, withdrawn);
		TestUsers.approved(entityManager, "프로필없는사자");

		PageResponse<AlumniResponse> result = search(ALL);

		assertThat(ids(result)).containsExactly(kim, me);
		assertThat(result.totalElements()).isEqualTo(2);
	}

	@Test
	void 검색어는_이름_직무_기술_공개된_회사에서_대소문자_구분없이_찾는다() {
		Long byName = alumni("검색사자");
		Long bySkill = TestUsers.approved(entityManager, "기술사자");
		TestProfiles.save(entityManager, TestProfiles.profile(bySkill).skills(List.of("SpringBoot", "Kotlin")));
		Long byCompany = alumni("회사사자");
		TestProfiles.currentCareer(entityManager, byCompany, "라이온컴퍼니", "백엔드 개발자");
		Long hiddenCompany = alumni("비공개사자");
		TestProfiles.currentCareer(entityManager, hiddenCompany, "라이온컴퍼니", "디자이너");
		TestProfiles.visibility(entityManager, hiddenCompany, "company_visibility", "PRIVATE");

		assertThat(ids(search(query("검색")))).containsExactly(byName);
		assertThat(ids(search(query("kotlin")))).containsExactly(bySkill);
		assertThat(ids(search(query("라이온컴퍼니")))).containsExactly(byCompany);
		assertThat(ids(search(query("디자이너")))).containsExactly(hiddenCompany);
		assertThat(ids(search(query("100%")))).isEmpty();
	}

	@Test
	void 같은_필터는_OR_다른_필터는_AND로_묶는다() {
		Long backend14 = TestUsers.create(entityManager, "십사기백엔드", UserStatus.APPROVED);
		entityManager.createNativeQuery("update users set generation = 14 where id = :id")
				.setParameter("id", backend14)
				.executeUpdate();
		TestProfiles.save(entityManager, TestProfiles.profile(backend14));
		Long designFront = TestUsers.approved(entityManager, "디자인프론트");
		TestProfiles.save(entityManager, TestProfiles.profile(designFront)
				.lionPart1(LionPart.DESIGN)
				.lionPart2(LionPart.FRONTEND)
				.status(ProfileStatus.STUDENT)
				.interests(List.of("DESIGN", "DATA_AI")));

		assertThat(ids(search(new AlumniSearchCondition(null, List.of(14), null, null, null))))
				.containsExactly(backend14);
		assertThat(ids(search(new AlumniSearchCondition(null, null, List.of(LionPart.FRONTEND), null, null))))
				.containsExactly(designFront);
		assertThat(ids(search(new AlumniSearchCondition(null, List.of(13, 14), List.of(LionPart.BACKEND), null, null))))
				.containsExactlyInAnyOrder(backend14, me);
		assertThat(ids(search(new AlumniSearchCondition(null, null, null, List.of(ProfileStatus.STUDENT), List.of("DATA_AI")))))
				.containsExactly(designFront);
		assertThat(ids(search(new AlumniSearchCondition(null, null, null, null, List.of("DATA")))))
				.isEmpty();
	}

	@Test
	void 목록에_관심_동문_여부와_공개된_현재_회사를_표시한다() {
		Long kim = alumni("김사자");
		TestProfiles.currentCareer(entityManager, kim, "라이온컴퍼니", "백엔드 개발자");
		Long lee = alumni("이사자");
		TestProfiles.currentCareer(entityManager, lee, "비밀회사", "백엔드 개발자");
		TestProfiles.visibility(entityManager, lee, "company_visibility", "CONNECTED");
		bookmarkService.bookmark(me, kim);

		List<AlumniResponse> items = search(ALL).items();

		AlumniResponse kimItem = find(items, kim);
		assertThat(kimItem.bookmarked()).isTrue();
		assertThat(kimItem.company()).isEqualTo("라이온컴퍼니");
		assertThat(kimItem.lionParts()).containsExactly(LionPart.BACKEND);
		AlumniResponse leeItem = find(items, lee);
		assertThat(leeItem.bookmarked()).isFalse();
		assertThat(leeItem.company()).isNull();

		TestProfiles.coffeeChat(entityManager, me, lee, CoffeeChatStatus.COMPLETED);
		assertThat(find(search(ALL).items(), lee).company()).isEqualTo("비밀회사");
	}

	@Test
	void 상세_조회는_공개_범위에_따라_연락처를_가린다() {
		Long kim = TestUsers.approved(entityManager, "김사자");
		TestProfiles.save(entityManager, TestProfiles.profile(kim).contactEmail("kim@test.com").phone("010-0000-0000"));
		TestProfiles.currentCareer(entityManager, kim, "라이온컴퍼니", "백엔드 개발자");

		AlumniDetailResponse stranger = alumniService.getDetail(me, kim);
		assertThat(stranger.name()).isEqualTo("김사자");
		assertThat(stranger.contactEmail()).isNull();
		assertThat(stranger.phone()).isNull();
		assertThat(stranger.links()).isEmpty();
		assertThat(stranger.careers()).extracting(AlumniDetailResponse.CareerResponse::company)
				.containsExactly("라이온컴퍼니");

		TestProfiles.coffeeChat(entityManager, kim, me, CoffeeChatStatus.ACCEPTED);
		AlumniDetailResponse connected = alumniService.getDetail(me, kim);
		assertThat(connected.contactEmail()).isEqualTo("kim@test.com");
		assertThat(connected.phone()).isEqualTo("010-0000-0000");

		TestProfiles.visibility(entityManager, kim, "company_visibility", "PRIVATE");
		TestProfiles.visibility(entityManager, kim, "links_visibility", "PRIVATE");
		AlumniDetailResponse hidden = alumniService.getDetail(me, kim);
		assertThat(hidden.careers()).extracting(AlumniDetailResponse.CareerResponse::company).containsOnlyNulls();
		assertThat(hidden.links()).isNull();

		AlumniDetailResponse owner = alumniService.getDetail(kim, kim);
		assertThat(owner.contactEmail()).isEqualTo("kim@test.com");
		assertThat(owner.careers()).extracting(AlumniDetailResponse.CareerResponse::company)
				.containsExactly("라이온컴퍼니");
	}

	@Test
	void 커피챗_수신이_켜져_있고_진행_중인_신청이_없을_때만_신청_가능하다() {
		Long kim = alumni("김사자");
		assertThat(alumniService.getDetail(me, kim).coffeeChatAvailable()).isFalse();

		entityManager.persist(CoffeeSetting.builder().userId(kim).modes(List.of(MeetingMode.ONLINE)).build());
		entityManager.flush();
		assertThat(alumniService.getDetail(me, kim).coffeeChatAvailable()).isTrue();
		assertThat(alumniService.getDetail(kim, kim).coffeeChatAvailable()).isFalse();

		TestProfiles.coffeeChat(entityManager, me, kim, CoffeeChatStatus.PENDING);
		assertThat(alumniService.getDetail(me, kim).coffeeChatAvailable()).isFalse();
	}

	@Test
	void 상세_조회_시_관심_동문_여부를_표시한다() {
		Long kim = alumni("김사자");
		bookmarkService.bookmark(me, kim);

		assertThat(alumniService.getDetail(me, kim).bookmarked()).isTrue();
	}

	@Test
	void 없거나_승인되지_않았거나_탈퇴했거나_프로필이_없는_회원은_404() {
		Long pending = TestUsers.create(entityManager, "대기사자", UserStatus.PENDING);
		TestProfiles.save(entityManager, TestProfiles.profile(pending));
		Long withdrawn = alumni("탈퇴사자");
		TestUsers.withdraw(entityManager, withdrawn);
		Long noProfile = TestUsers.approved(entityManager, "프로필없는사자");

		for (Long target : new Long[] {pending, withdrawn, noProfile, 999_999L}) {
			assertThatThrownBy(() -> alumniService.getDetail(me, target))
					.isInstanceOf(BusinessException.class)
					.extracting("errorCode")
					.isEqualTo(AlumniErrorCode.USER_NOT_FOUND);
		}
	}

	private Long alumni(String name) {
		Long userId = TestUsers.approved(entityManager, name);
		TestProfiles.save(entityManager, TestProfiles.profile(userId));
		return userId;
	}

	private PageResponse<AlumniResponse> search(AlumniSearchCondition condition) {
		return alumniService.search(me, condition, PageRequest.of(0, 20));
	}

	private AlumniSearchCondition query(String q) {
		return new AlumniSearchCondition(q, null, null, null, null);
	}

	private List<Long> ids(PageResponse<AlumniResponse> result) {
		return result.items().stream().map(AlumniResponse::id).toList();
	}

	private AlumniResponse find(List<AlumniResponse> items, Long userId) {
		return items.stream().filter(item -> item.id().equals(userId)).findFirst().orElseThrow();
	}
}
