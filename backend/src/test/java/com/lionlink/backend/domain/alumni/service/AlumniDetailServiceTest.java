package com.lionlink.backend.domain.alumni.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionlink.backend.domain.alumni.dto.response.AlumniDetailResponse;
import com.lionlink.backend.domain.alumni.dto.response.AlumniDetailResponse.ChatStatus;
import com.lionlink.backend.domain.alumni.dto.response.AlumniDetailResponse.CoffeeChatAvailability;
import com.lionlink.backend.domain.alumni.dto.response.AlumniDetailResponse.Commonality;
import com.lionlink.backend.domain.alumni.dto.response.AlumniDetailResponse.CommonalityType;
import com.lionlink.backend.domain.alumni.exception.AlumniErrorCode;
import com.lionlink.backend.domain.chat.entity.ChatRoom;
import com.lionlink.backend.domain.chat.entity.ChatRoomStatus;
import com.lionlink.backend.domain.chat.entity.ChatRoomType;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChat;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatMember;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatMemberRole;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatStatus;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeSetting;
import com.lionlink.backend.domain.coffeechat.entity.MeetingMode;
import com.lionlink.backend.domain.coffeechat.entity.ReceiveMode;
import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.section.activity.Activity;
import com.lionlink.backend.domain.profile.section.award.Award;
import com.lionlink.backend.domain.profile.section.certification.Certification;
import com.lionlink.backend.domain.profile.section.education.Degree;
import com.lionlink.backend.domain.profile.section.education.Education;
import com.lionlink.backend.domain.profile.section.language.Language;
import com.lionlink.backend.domain.profile.section.language.LanguageLevel;
import com.lionlink.backend.domain.profile.section.lionactivity.LionActivity;
import com.lionlink.backend.domain.profile.section.lionactivity.LionRole;
import com.lionlink.backend.domain.profile.section.lionproject.LionEventType;
import com.lionlink.backend.domain.profile.section.lionproject.LionProject;
import com.lionlink.backend.domain.profile.section.project.Project;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.support.TestProfiles;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AlumniDetailServiceTest {

	@Autowired
	private AlumniDetailService alumniDetailService;

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
	void 프로필과_섹션_9개를_모두_담는다() {
		Long kim = TestUsers.approved(entityManager, "김사자");
		TestProfiles.save(entityManager, TestProfiles.profile(kim).bio("서버 개발을 좋아해요").minor("경영학").region("서울"));
		TestProfiles.currentCareer(entityManager, kim, "라이온컴퍼니", "백엔드 개발자");
		persist(Education.builder().userId(kim).school("중앙대학교").major("소프트웨어").degree(Degree.BACHELOR)
				.startYm(YearMonth.of(2019, 3)).build());
		persist(LionActivity.builder().userId(kim).generation(13).role(LionRole.LION).part1(LionPart.BACKEND).isDefault(true).build());
		persist(LionProject.builder().userId(kim).name("라이언링크").eventType(LionEventType.JUNGKATHON).generation(13).build());
		persist(Project.builder().userId(kim).name("개인 프로젝트").startYm(YearMonth.of(2024, 1)).tags(List.of("Spring")).build());
		persist(Certification.builder().userId(kim).name("정보처리기사").organization("한국산업인력공단").date(YearMonth.of(2023, 6)).build());
		persist(Award.builder().userId(kim).name("최우수상").organization("중커톤").date(YearMonth.of(2025, 11)).build());
		persist(Activity.builder().userId(kim).name("개발 동아리").organization("중앙대").startYm(YearMonth.of(2020, 3)).build());
		persist(Language.builder().userId(kim).language("영어").level(LanguageLevel.BUSINESS).build());

		AlumniDetailResponse detail = alumniDetailService.getDetail(me, kim);

		assertThat(detail.userId()).isEqualTo(kim);
		assertThat(detail.bio()).isEqualTo("서버 개발을 좋아해요");
		assertThat(detail.minor()).isEqualTo("경영학");
		assertThat(detail.careers()).extracting(AlumniDetailResponse.CareerResponse::jobTitle).containsExactly("백엔드 개발자");
		assertThat(detail.educations()).hasSize(1);
		assertThat(detail.lionActivities()).hasSize(1);
		assertThat(detail.lionProjects()).extracting(AlumniDetailResponse.LionProjectResponse::name).containsExactly("라이언링크");
		assertThat(detail.projects()).hasSize(1);
		assertThat(detail.certifications()).extracting(AlumniDetailResponse.CertificationResponse::issuer)
				.containsExactly("한국산업인력공단");
		assertThat(detail.awards()).extracting(AlumniDetailResponse.AwardResponse::host).containsExactly("중커톤");
		assertThat(detail.activities()).hasSize(1);
		assertThat(detail.languages()).extracting(AlumniDetailResponse.LanguageResponse::level)
				.containsExactly(LanguageLevel.BUSINESS);
		assertThat(detail.isStaff()).isFalse();
	}

	@Test
	void 공개_범위로_가려진_연락처와_링크는_빼고_비공개_회사는_companyHidden으로_알린다() {
		Long kim = TestUsers.approved(entityManager, "김사자");
		TestProfiles.save(entityManager, TestProfiles.profile(kim).contactEmail("kim@test.com").phone("010-0000-0000"));
		TestProfiles.currentCareer(entityManager, kim, "라이온컴퍼니", "백엔드 개발자");

		AlumniDetailResponse stranger = alumniDetailService.getDetail(me, kim);
		assertThat(stranger.contact().email()).isNull();
		assertThat(stranger.contact().phone()).isNull();
		assertThat(stranger.links()).isEmpty();
		assertThat(stranger.careers().get(0).company()).isEqualTo("라이온컴퍼니");
		assertThat(stranger.careers().get(0).companyHidden()).isFalse();

		TestProfiles.coffeeChat(entityManager, kim, me, CoffeeChatStatus.ACCEPTED);
		AlumniDetailResponse connected = alumniDetailService.getDetail(me, kim);
		assertThat(connected.contact().email()).isEqualTo("kim@test.com");
		assertThat(connected.contact().phone()).isEqualTo("010-0000-0000");

		TestProfiles.visibility(entityManager, kim, "company_visibility", "PRIVATE");
		TestProfiles.visibility(entityManager, kim, "links_visibility", "PRIVATE");
		AlumniDetailResponse hidden = alumniDetailService.getDetail(me, kim);
		assertThat(hidden.careers().get(0).company()).isNull();
		assertThat(hidden.careers().get(0).companyHidden()).isTrue();
		assertThat(hidden.careers().get(0).jobTitle()).isEqualTo("백엔드 개발자");
		assertThat(hidden.links()).isNull();

		AlumniDetailResponse owner = alumniDetailService.getDetail(kim, kim);
		assertThat(owner.contact().email()).isEqualTo("kim@test.com");
		assertThat(owner.careers().get(0).company()).isEqualTo("라이온컴퍼니");
		assertThat(owner.links()).isEmpty();
	}

	@Test
	void 연결된_동문은_커피챗이_수락된_적_있거나_메시지_대화_중인_사이다() {
		Long canceledAfterAccept = alumniWithEmail("취소사자");
		Long acceptedId = TestProfiles.coffeeChat(entityManager, me, canceledAfterAccept, CoffeeChatStatus.ACCEPTED);
		update("update coffee_chats set status = 'CANCELED', open_flag = null where id = " + acceptedId);
		Long pending = alumniWithEmail("대기사자");
		TestProfiles.coffeeChat(entityManager, me, pending, CoffeeChatStatus.PENDING);
		Long chatting = alumniWithEmail("대화사자");
		room(me, chatting, ChatRoomStatus.ACTIVE, me);

		Long groupReceiver = alumniWithEmail("모임선배");
		Long representative = TestUsers.approved(entityManager, "대표사자");
		Long groupChat = TestProfiles.coffeeChat(entityManager, representative, groupReceiver, CoffeeChatStatus.SCHEDULED);
		member(groupChat, me, false);
		Long leftReceiver = alumniWithEmail("빠진모임선배");
		Long leftChat = TestProfiles.coffeeChat(entityManager, representative, leftReceiver, CoffeeChatStatus.SCHEDULED);
		member(leftChat, me, true);

		assertThat(email(canceledAfterAccept)).isNotNull();
		assertThat(email(chatting)).isNotNull();
		assertThat(email(groupReceiver)).isNotNull();
		assertThat(email(pending)).isNull();
		assertThat(email(leftReceiver)).isNull();
	}

	@Test
	void 커피챗_카드에_설정_요약과_진행_중_여부와_대기_중_신청을_담는다() {
		Long kim = alumni("김사자");
		entityManager.persist(CoffeeSetting.builder().userId(kim).topics(List.of("CAREER"))
				.modes(List.of(MeetingMode.ONLINE, MeetingMode.OFFLINE)).preferredArea("강남").build());
		entityManager.flush();

		AlumniDetailResponse.CoffeeChat available = alumniDetailService.getDetail(me, kim).coffeeChat();
		assertThat(available.availability()).isEqualTo(CoffeeChatAvailability.AVAILABLE);
		assertThat(available.acceptGroup()).isTrue();
		assertThat(available.topics()).containsExactly("CAREER");
		assertThat(available.modes()).containsExactly(MeetingMode.ONLINE, MeetingMode.OFFLINE);
		assertThat(available.preferredArea()).isEqualTo("강남");
		assertThat(available.pendingCoffeeChatId()).isNull();

		Long pendingId = TestProfiles.coffeeChat(entityManager, me, kim, CoffeeChatStatus.PENDING);
		AlumniDetailResponse.CoffeeChat inProgress = alumniDetailService.getDetail(me, kim).coffeeChat();
		assertThat(inProgress.availability()).isEqualTo(CoffeeChatAvailability.IN_PROGRESS);
		assertThat(inProgress.pendingCoffeeChatId()).isEqualTo(pendingId);

		Long lee = alumni("이사자");
		TestProfiles.coffeeSetting(entityManager, lee, ReceiveMode.ON, true);
		TestProfiles.coffeePeriod(entityManager, lee, today, today.plusDays(2));
		AlumniDetailResponse.CoffeeChat resting = alumniDetailService.getDetail(me, lee).coffeeChat();
		assertThat(resting.availability()).isEqualTo(CoffeeChatAvailability.RESTING);
		assertThat(resting.nextAvailableDate()).isEqualTo(today.plusDays(3));

		Long representative = TestUsers.approved(entityManager, "대표사자");
		Long groupChat = TestProfiles.coffeeChat(entityManager, representative, lee, CoffeeChatStatus.PENDING);
		member(groupChat, me, false);
		assertThat(alumniDetailService.getDetail(me, lee).coffeeChat().availability()).isEqualTo(CoffeeChatAvailability.IN_PROGRESS);

		Long noSetting = alumni("설정없는사자");
		AlumniDetailResponse.CoffeeChat none = alumniDetailService.getDetail(me, noSetting).coffeeChat();
		assertThat(none.availability()).isEqualTo(CoffeeChatAvailability.NOT_ACCEPTING);
		assertThat(none.topics()).isEmpty();
		assertThat(none.modes()).isEmpty();
	}

	@Test
	void 메시지_상태와_거절_후_다시_보낼_수_있는_날짜를_담는다() {
		Long none = alumni("처음사자");
		Long requested = alumni("요청사자");
		room(me, requested, ChatRoomStatus.REQUESTED, me);
		Long active = alumni("대화사자");
		room(active, me, ChatRoomStatus.ACTIVE, active);
		Long declined = alumni("거절사자");
		Long declinedRoom = room(me, declined, ChatRoomStatus.DECLINED, me);
		update("update chat_rooms set declined_at = dateadd('DAY', -2, current_timestamp) where id = " + declinedRoom);
		Long longAgo = alumni("오래전거절사자");
		Long longAgoRoom = room(me, longAgo, ChatRoomStatus.DECLINED, me);
		update("update chat_rooms set declined_at = dateadd('DAY', -8, current_timestamp) where id = " + longAgoRoom);

		assertThat(chat(none)).isEqualTo(new AlumniDetailResponse.Chat(ChatStatus.NONE, null));
		assertThat(chat(requested).status()).isEqualTo(ChatStatus.REQUESTED);
		assertThat(chat(active).status()).isEqualTo(ChatStatus.ACTIVE);
		assertThat(chat(declined)).isEqualTo(new AlumniDetailResponse.Chat(ChatStatus.NONE, today.plusDays(5)));
		assertThat(chat(longAgo)).isEqualTo(new AlumniDetailResponse.Chat(ChatStatus.NONE, null));
	}

	@Test
	void 나와의_공통점을_정해진_순서로_보여주고_비공개_회사는_뺀다() {
		Long viewer = TestUsers.approved(entityManager, "보는사자");
		TestProfiles.save(entityManager, TestProfiles.profile(viewer).interests(List.of("BACKEND", "DATA_AI")));
		TestProfiles.currentCareer(entityManager, viewer, "라이온컴퍼니", "백엔드 개발자");
		persist(LionProject.builder().userId(viewer).name("라이언링크").eventType(LionEventType.JUNGKATHON).generation(13).build());

		Long kim = TestUsers.approved(entityManager, "김사자");
		TestProfiles.save(entityManager, TestProfiles.profile(kim)
				.lionPart2(LionPart.FRONTEND)
				.interests(List.of("DATA_AI", "DESIGN")));
		TestProfiles.currentCareer(entityManager, kim, "라이온컴퍼니", "백엔드 개발자");
		persist(LionProject.builder().userId(kim).name("라이언링크").eventType(LionEventType.JUNGKATHON).generation(13).build());

		assertThat(alumniDetailService.getDetail(viewer, kim).commonalities()).containsExactly(
				new Commonality(CommonalityType.SAME_LION_PROJECT, "13기 라이언링크"),
				new Commonality(CommonalityType.SAME_COMPANY, "라이온컴퍼니"),
				new Commonality(CommonalityType.SAME_GENERATION, "13기"),
				new Commonality(CommonalityType.SAME_PART, "백엔드 파트"),
				new Commonality(CommonalityType.SHARED_INTEREST, "데이터·AI")
		);

		TestProfiles.visibility(entityManager, kim, "company_visibility", "PRIVATE");
		assertThat(alumniDetailService.getDetail(viewer, kim).commonalities())
				.extracting(Commonality::type)
				.doesNotContain(CommonalityType.SAME_COMPANY);
	}

	@Test
	void 함께_아는_동문_수는_탈퇴한_회원을_빼고_센다() {
		Long kim = alumni("김사자");
		Long both = TestUsers.approved(entityManager, "둘다아는사자");
		Long withdrawn = TestUsers.approved(entityManager, "탈퇴사자");
		Long onlyMine = TestUsers.approved(entityManager, "나만아는사자");
		Long onlyKims = TestUsers.approved(entityManager, "김만아는사자");
		for (Long friend : List.of(both, withdrawn, onlyMine)) {
			TestProfiles.coffeeChat(entityManager, me, friend, CoffeeChatStatus.COMPLETED);
		}
		for (Long friend : List.of(both, withdrawn, onlyKims)) {
			TestProfiles.coffeeChat(entityManager, friend, kim, CoffeeChatStatus.COMPLETED);
		}
		TestProfiles.coffeeChat(entityManager, me, kim, CoffeeChatStatus.COMPLETED);
		TestUsers.withdraw(entityManager, withdrawn);

		assertThat(alumniDetailService.getDetail(me, kim).mutualCount()).isEqualTo(1);
	}

	@Test
	void 운영진_이력이_있으면_운영진으로_표시한다() {
		Long staff = alumni("운영진사자");
		persist(LionActivity.builder().userId(staff).generation(12).role(LionRole.STAFF).part1(LionPart.BACKEND).build());
		Long president = alumni("대표사자");
		persist(LionActivity.builder().userId(president).generation(12).role(LionRole.PRESIDENT).part1(LionPart.BACKEND).build());
		Long lion = alumni("아기사자");
		persist(LionActivity.builder().userId(lion).generation(13).role(LionRole.LION).part1(LionPart.BACKEND).isDefault(true).build());

		assertThat(alumniDetailService.getDetail(me, staff).isStaff()).isTrue();
		assertThat(alumniDetailService.getDetail(me, president).isStaff()).isTrue();
		assertThat(alumniDetailService.getDetail(me, lion).isStaff()).isFalse();
	}

	@Test
	void 내_프로필은_관계_정보를_비운다() {
		TestProfiles.coffeeSetting(entityManager, me, ReceiveMode.ON, true);

		AlumniDetailResponse mine = alumniDetailService.getDetail(me, me);

		assertThat(mine.commonalities()).isEmpty();
		assertThat(mine.mutualCount()).isZero();
		assertThat(mine.bookmarked()).isFalse();
		assertThat(mine.chat().status()).isEqualTo(ChatStatus.NONE);
		assertThat(mine.coffeeChat().availability()).isEqualTo(CoffeeChatAvailability.AVAILABLE);
		assertThat(mine.coffeeChat().pendingCoffeeChatId()).isNull();
	}

	@Test
	void 관심_동문_여부를_표시한다() {
		Long kim = alumni("김사자");
		bookmarkService.bookmark(me, kim);

		assertThat(alumniDetailService.getDetail(me, kim).bookmarked()).isTrue();
	}

	@Test
	void 없거나_승인되지_않았거나_탈퇴했거나_프로필이_없는_회원은_404() {
		Long pending = TestUsers.create(entityManager, "대기사자", UserStatus.PENDING);
		TestProfiles.save(entityManager, TestProfiles.profile(pending));
		Long withdrawn = alumni("탈퇴사자");
		TestUsers.withdraw(entityManager, withdrawn);
		Long noProfile = TestUsers.approved(entityManager, "프로필없는사자");

		for (Long target : new Long[] {pending, withdrawn, noProfile, 999_999L}) {
			assertThatThrownBy(() -> alumniDetailService.getDetail(me, target))
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

	private Long alumniWithEmail(String name) {
		Long userId = TestUsers.approved(entityManager, name);
		TestProfiles.save(entityManager, TestProfiles.profile(userId).contactEmail(name + "@test.com"));
		return userId;
	}

	private String email(Long userId) {
		return alumniDetailService.getDetail(me, userId).contact().email();
	}

	private AlumniDetailResponse.Chat chat(Long userId) {
		return alumniDetailService.getDetail(me, userId).chat();
	}

	private void persist(Object entity) {
		entityManager.persist(entity);
		entityManager.flush();
		entityManager.clear();
	}

	private void member(Long coffeeChatId, Long userId, boolean leftBeforeAccept) {
		entityManager.persist(CoffeeChatMember.builder()
				.coffeeChat(entityManager.find(CoffeeChat.class, coffeeChatId))
				.userId(userId)
				.role(CoffeeChatMemberRole.MEMBER)
				.build());
		entityManager.flush();
		if (leftBeforeAccept) {
			update("update coffee_chat_members set left_at = dateadd('DAY', -1, current_timestamp) where user_id = " + userId
					+ " and coffee_chat_id = " + coffeeChatId);
		}
		entityManager.clear();
	}

	private Long room(Long userA, Long userB, ChatRoomStatus status, Long requestedBy) {
		ChatRoom room = ChatRoom.builder()
				.type(ChatRoomType.DIRECT)
				.userAId(userA)
				.userBId(userB)
				.status(status)
				.requestedBy(requestedBy)
				.build();
		entityManager.persist(room);
		entityManager.flush();
		entityManager.clear();
		return room.getId();
	}

	private void update(String sql) {
		entityManager.flush();
		entityManager.createNativeQuery(sql).executeUpdate();
		entityManager.clear();
	}
}
