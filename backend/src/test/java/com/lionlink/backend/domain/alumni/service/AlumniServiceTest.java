package com.lionlink.backend.domain.alumni.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionlink.backend.domain.alumni.dto.request.AlumniSearchCondition;
import com.lionlink.backend.domain.alumni.dto.request.AlumniSearchCondition.AlumniSort;
import com.lionlink.backend.domain.alumni.dto.response.AlumniDetailResponse;
import com.lionlink.backend.domain.alumni.dto.response.AlumniResponse;
import com.lionlink.backend.domain.alumni.dto.response.BookmarkedAlumniResponse;
import com.lionlink.backend.domain.alumni.exception.AlumniErrorCode;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatStatus;
import com.lionlink.backend.domain.coffeechat.entity.ReceiveMode;
import com.lionlink.backend.domain.profile.CoffeeAvailability;
import com.lionlink.backend.domain.profile.CoffeeAvailability.Availability;
import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.ProfileStatus;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.response.PageResponse;
import com.lionlink.backend.support.TestProfiles;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
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

	private static final AlumniSearchCondition ALL = condition(false, AlumniSort.LATEST);

	@Autowired
	private AlumniService alumniService;

	@Autowired
	private BookmarkService bookmarkService;

	@Autowired
	private EntityManager entityManager;

	private final LocalDate today = LocalDate.now();

	private Long me;

	@BeforeEach
	void setUp() {
		me = TestUsers.approved(entityManager, "나사자");
		TestProfiles.save(entityManager, TestProfiles.profile(me));
	}

	@Test
	void 승인되고_프로필이_있는_활성_회원만_조회하고_나는_뺀다() {
		Long kim = alumni("김사자");
		Long pending = TestUsers.create(entityManager, "대기사자", UserStatus.PENDING);
		TestProfiles.save(entityManager, TestProfiles.profile(pending));
		Long withdrawn = alumni("탈퇴사자");
		TestUsers.withdraw(entityManager, withdrawn);
		TestUsers.approved(entityManager, "프로필없는사자");

		PageResponse<AlumniResponse> result = search(ALL);

		assertThat(ids(result)).containsExactly(kim);
		assertThat(result.totalElements()).isEqualTo(1);
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
		Long backend13 = alumni("십삼기백엔드");
		Long backend14 = alumni("십사기백엔드");
		generation(backend14, 14);
		Long designFront = TestUsers.approved(entityManager, "디자인프론트");
		TestProfiles.save(entityManager, TestProfiles.profile(designFront)
				.lionPart1(LionPart.DESIGN)
				.lionPart2(LionPart.FRONTEND)
				.status(ProfileStatus.STUDENT)
				.interests(List.of("DESIGN", "DATA_AI")));

		assertThat(ids(search(filter(List.of(14), null, null, null)))).containsExactly(backend14);
		assertThat(ids(search(filter(null, List.of(LionPart.FRONTEND), null, null)))).containsExactly(designFront);
		assertThat(ids(search(filter(List.of(13, 14), List.of(LionPart.BACKEND), null, null))))
				.containsExactlyInAnyOrder(backend13, backend14);
		assertThat(ids(search(filter(null, null, List.of(ProfileStatus.STUDENT), List.of("DATA_AI")))))
				.containsExactly(designFront);
		assertThat(ids(search(filter(null, null, null, List.of("DATA"))))).isEmpty();
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
	void 목록에_소개와_오늘_기준_커피챗_수신_상태를_표시한다() {
		Long resting = TestUsers.approved(entityManager, "쉬는사자");
		TestProfiles.save(entityManager, TestProfiles.profile(resting).bio("서버 개발 이야기를 좋아해요"));
		TestProfiles.coffeeSetting(entityManager, resting, ReceiveMode.ON, false);
		TestProfiles.coffeePeriod(entityManager, resting, today.minusDays(1), today.plusDays(3));
		Long noSetting = alumni("설정없는사자");

		List<AlumniResponse> items = search(ALL).items();

		AlumniResponse restingItem = find(items, resting);
		assertThat(restingItem.bio()).isEqualTo("서버 개발 이야기를 좋아해요");
		assertThat(restingItem.coffeeChat())
				.isEqualTo(new CoffeeAvailability(Availability.RESTING, today.plusDays(4), false));
		assertThat(find(items, noSetting).coffeeChat()).isEqualTo(CoffeeAvailability.NOT_ACCEPTING);
	}

	@Test
	void 오늘_커피챗을_받는_동문만_조회한다() {
		Long on = receiver("받기사자", ReceiveMode.ON);
		Long onResting = receiver("쉬는사자", ReceiveMode.ON);
		TestProfiles.coffeePeriod(entityManager, onResting, today, today.plusDays(2));
		Long onRestLater = receiver("나중에쉬는사자", ReceiveMode.ON);
		TestProfiles.coffeePeriod(entityManager, onRestLater, today.plusDays(1), today.plusDays(3));
		Long offOpen = receiver("기간받는사자", ReceiveMode.OFF);
		TestProfiles.coffeePeriod(entityManager, offOpen, today.minusDays(2), today);
		Long offClosed = receiver("안받는사자", ReceiveMode.OFF);
		TestProfiles.coffeePeriod(entityManager, offClosed, today.plusDays(1), today.plusDays(5));
		alumni("설정없는사자");

		PageResponse<AlumniResponse> result = search(condition(true, AlumniSort.LATEST));

		assertThat(ids(result)).containsExactly(offOpen, onRestLater, on);
		assertThat(result.totalElements()).isEqualTo(3);
		assertThat(search(ALL).items().stream().filter(item -> item.coffeeChat().available()).map(AlumniResponse::id))
				.containsExactlyElementsOf(ids(result));
		assertThat(find(search(ALL).items(), offClosed).coffeeChat().nextAvailableDate()).isEqualTo(today.plusDays(1));
	}

	@Test
	void 추천순은_받는_중_나와_같은_파트_최근_가입_순서다() {
		Long frontReceiving = alumni("프론트받는사자", LionPart.FRONTEND);
		TestProfiles.coffeeSetting(entityManager, frontReceiving, ReceiveMode.ON, true);
		Long backReceiving = alumni("백엔드받는사자", LionPart.BACKEND);
		TestProfiles.coffeeSetting(entityManager, backReceiving, ReceiveMode.ON, true);
		Long back = alumni("백엔드사자", LionPart.BACKEND);
		Long front = alumni("프론트사자", LionPart.FRONTEND);
		Long designReceiving = alumni("디자인받는사자", LionPart.DESIGN);
		TestProfiles.coffeeSetting(entityManager, designReceiving, ReceiveMode.ON, true);

		assertThat(ids(search(condition(false, AlumniSort.RECOMMEND))))
				.containsExactly(backReceiving, designReceiving, frontReceiving, back, front);
	}

	@Test
	void 기수순은_기수_오름차순이고_최근_가입순은_가입_역순이다() {
		Long gen14 = alumni("십사기사자");
		generation(gen14, 14);
		Long gen12 = alumni("십이기사자");
		generation(gen12, 12);
		Long gen13 = alumni("십삼기사자");

		assertThat(ids(search(condition(false, AlumniSort.GENERATION)))).containsExactly(gen12, gen13, gen14);
		assertThat(ids(search(condition(false, AlumniSort.LATEST)))).containsExactly(gen13, gen12, gen14);
	}

	@Test
	void 관심_동문은_동문_카드_형식으로_최근_저장순이고_탈퇴한_동문은_뺀다() {
		Long first = alumni("김사자");
		Long second = alumni("이사자");
		TestProfiles.coffeeSetting(entityManager, second, ReceiveMode.ON, true);
		Long withdrawn = alumni("탈퇴사자");
		Long noProfile = TestUsers.approved(entityManager, "프로필없는사자");
		for (Long target : List.of(first, second, withdrawn, noProfile)) {
			bookmarkService.bookmark(me, target);
		}
		TestUsers.withdraw(entityManager, withdrawn);

		PageResponse<BookmarkedAlumniResponse> result = alumniService.getBookmarks(me, PageRequest.of(0, 20));

		assertThat(result.items()).extracting(item -> item.alumni().id()).containsExactly(second, first);
		assertThat(result.totalElements()).isEqualTo(2);
		assertThat(result.hasNext()).isFalse();
		BookmarkedAlumniResponse latest = result.items().get(0);
		assertThat(latest.alumni().name()).isEqualTo("이사자");
		assertThat(latest.alumni().bookmarked()).isTrue();
		assertThat(latest.alumni().coffeeChat().availability()).isEqualTo(Availability.AVAILABLE);
		assertThat(latest.bookmarkedAt()).isNotNull();
		assertThat(alumniService.countBookmarks(me)).isEqualTo(2);

		PageResponse<BookmarkedAlumniResponse> firstPage = alumniService.getBookmarks(me, PageRequest.of(0, 1));
		assertThat(firstPage.items()).hasSize(1);
		assertThat(firstPage.totalElements()).isEqualTo(2);
		assertThat(firstPage.hasNext()).isTrue();
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
	void 오늘_커피챗을_받고_진행_중인_신청이_없을_때만_신청_가능하다() {
		Long kim = alumni("김사자");
		assertThat(alumniService.getDetail(me, kim).coffeeChatAvailable()).isFalse();

		TestProfiles.coffeeSetting(entityManager, kim, ReceiveMode.ON, true);
		assertThat(alumniService.getDetail(me, kim).coffeeChatAvailable()).isTrue();
		assertThat(alumniService.getDetail(kim, kim).coffeeChatAvailable()).isFalse();

		TestProfiles.coffeePeriod(entityManager, kim, today, today.plusDays(1));
		assertThat(alumniService.getDetail(me, kim).coffeeChatAvailable()).isFalse();

		Long lee = receiver("이사자", ReceiveMode.OFF);
		assertThat(alumniService.getDetail(me, lee).coffeeChatAvailable()).isFalse();
		TestProfiles.coffeePeriod(entityManager, lee, today, today);
		assertThat(alumniService.getDetail(me, lee).coffeeChatAvailable()).isTrue();

		TestProfiles.coffeeChat(entityManager, me, lee, CoffeeChatStatus.PENDING);
		assertThat(alumniService.getDetail(me, lee).coffeeChatAvailable()).isFalse();
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
		return alumni(name, LionPart.BACKEND);
	}

	private Long alumni(String name, LionPart part) {
		Long userId = TestUsers.approved(entityManager, name);
		TestProfiles.save(entityManager, TestProfiles.profile(userId).lionPart1(part));
		return userId;
	}

	private Long receiver(String name, ReceiveMode mode) {
		Long userId = alumni(name);
		TestProfiles.coffeeSetting(entityManager, userId, mode, true);
		return userId;
	}

	private void generation(Long userId, int generation) {
		entityManager.flush();
		entityManager.createNativeQuery("update users set generation = :generation where id = :id")
				.setParameter("generation", generation)
				.setParameter("id", userId)
				.executeUpdate();
		entityManager.clear();
	}

	private PageResponse<AlumniResponse> search(AlumniSearchCondition condition) {
		return alumniService.search(me, condition, PageRequest.of(0, 20));
	}

	private static AlumniSearchCondition condition(boolean available, AlumniSort sort) {
		return new AlumniSearchCondition(null, null, null, null, null, available, sort);
	}

	private AlumniSearchCondition filter(List<Integer> generations, List<LionPart> parts, List<ProfileStatus> statuses,
			List<String> interests) {
		return new AlumniSearchCondition(null, generations, parts, statuses, interests, false, AlumniSort.LATEST);
	}

	private AlumniSearchCondition query(String q) {
		return new AlumniSearchCondition(q, null, null, null, null, false, AlumniSort.LATEST);
	}

	private List<Long> ids(PageResponse<AlumniResponse> result) {
		return result.items().stream().map(AlumniResponse::id).toList();
	}

	private AlumniResponse find(List<AlumniResponse> items, Long userId) {
		return items.stream().filter(item -> item.id().equals(userId)).findFirst().orElseThrow();
	}
}
