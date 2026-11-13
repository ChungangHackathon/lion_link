package com.lionlink.backend.domain.community.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionlink.backend.domain.alumni.exception.AlumniErrorCode;
import com.lionlink.backend.domain.coffeechat.entity.ReceiveMode;
import com.lionlink.backend.domain.community.exception.CommunityErrorCode;
import com.lionlink.backend.domain.community.meetup.entity.Meetup;
import com.lionlink.backend.domain.community.meetup.repository.MeetupMemberRepository;
import com.lionlink.backend.domain.community.meetup.dto.request.MeetupInviteRequest;
import com.lionlink.backend.domain.community.meetup.repository.MeetupRepository;
import com.lionlink.backend.domain.community.meetup.service.MeetupService;
import com.lionlink.backend.domain.community.post.dto.request.PostCreateRequest;
import com.lionlink.backend.domain.community.post.dto.request.PostUpdateRequest;
import com.lionlink.backend.domain.community.post.entity.Post;
import com.lionlink.backend.domain.community.post.entity.PostCategory;
import com.lionlink.backend.domain.community.post.repository.PostRepository;
import com.lionlink.backend.domain.community.recruit.repository.CoffeeChatRecruitRepository;
import com.lionlink.backend.domain.community.recruit.repository.RecruitMemberRepository;
import com.lionlink.backend.domain.community.recruit.service.RecruitService;
import com.lionlink.backend.domain.notification.entity.Notification;
import com.lionlink.backend.domain.notification.repository.NotificationRepository;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.ErrorCode;
import com.lionlink.backend.support.TestPosts;
import com.lionlink.backend.support.TestProfiles;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PostServiceTest {

	@Autowired
	private PostService postService;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private MeetupRepository meetupRepository;

	@Autowired
	private MeetupMemberRepository meetupMemberRepository;

	@Autowired
	private RecruitMemberRepository recruitMemberRepository;

	@Autowired
	private CoffeeChatRecruitRepository coffeeChatRecruitRepository;

	@Autowired
	private MeetupService meetupService;

	@Autowired
	private RecruitService recruitService;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private EntityManager entityManager;

	private Long me;

	@BeforeEach
	void setUp() {
		me = TestUsers.approved(entityManager, "나사자");
	}

	@Test
	void 일반_게시글을_작성한다() {
		PostCreateRequest request = new PostCreateRequest(PostCategory.CONTEST, "  핀테크 공모전  ", " 팀원 구해요 ",
				List.of("https://example.com"), false, LocalDate.now().plusDays(5), null, null, null);

		Long postId = postService.create(me, request).id();

		Post post = postRepository.findById(postId).orElseThrow();
		assertThat(post.getTitle()).isEqualTo("핀테크 공모전");
		assertThat(post.getContent()).isEqualTo("팀원 구해요");
		assertThat(post.getLinks()).containsExactly("https://example.com");
		assertThat(post.getDeadline()).isEqualTo(LocalDate.now().plusDays(5));
		assertThat(post.getAuthorId()).isEqualTo(me);
		assertThat(post.getUpdatedAt()).isNull();
	}

	@Test
	void 익명_글은_자유_게시판에서만_쓸_수_있다() {
		PostCreateRequest free = new PostCreateRequest(PostCategory.FREE, "익명 고민", "고민이 있어요.", null, true, null, null, null, null);
		PostCreateRequest contest = new PostCreateRequest(PostCategory.CONTEST, "익명 공모전", "같이 해요.", null, true, null, null, null, null);

		assertThat(postRepository.findById(postService.create(me, free).id()).orElseThrow().isAnonymous()).isTrue();
		assertError(() -> postService.create(me, contest), CommunityErrorCode.ANONYMOUS_NOT_ALLOWED);
	}

	@Test
	void 카테고리와_맞지_않는_추가_정보는_거절한다() {
		PostCreateRequest meetupWithoutDetail = TestPosts.general(PostCategory.MEETUP, "번개");
		PostCreateRequest freeWithMeetup = new PostCreateRequest(PostCategory.FREE, "자유", "본문", null, false, null,
				TestPosts.meetup("번개", 4, null).meetup(), null, null);
		PostCreateRequest materialWithDeadline = new PostCreateRequest(PostCategory.MATERIAL, "족보", "본문", null, false,
				LocalDate.now(), null, null, null);

		for (PostCreateRequest request : List.of(meetupWithoutDetail, freeWithMeetup, materialWithDeadline)) {
			assertError(() -> postService.create(me, request), CommunityErrorCode.INVALID_CATEGORY_DETAIL);
		}
	}

	@Test
	void 번개는_작성자와_함께하는_동문을_참여자로_등록한다() {
		Long kim = TestUsers.approved(entityManager, "김사자");
		Long lee = TestUsers.approved(entityManager, "이사자");

		Long postId = postService.create(me, TestPosts.meetup("금요일 번개", 6, List.of(kim, lee))).id();

		assertThat(meetupRepository.findById(postId)).isPresent();
		assertThat(meetupMemberRepository.findByMeetupPostIdOrderByJoinedAtAscUserIdAsc(postId))
				.extracting(member -> member.getUserId())
				.containsExactlyInAnyOrder(me, kim, lee);
	}

	@Test
	void 번개_일정과_함께하는_동문을_검증한다() {
		Long kim = TestUsers.approved(entityManager, "김사자");
		Long lee = TestUsers.approved(entityManager, "이사자");
		Long pending = TestUsers.create(entityManager, "대기사자", UserStatus.PENDING);
		PostCreateRequest soon = new PostCreateRequest(PostCategory.MEETUP, "곧 번개", "본문", null, false, null,
				new PostCreateRequest.MeetupRequest(LocalDateTime.now().plusMinutes(30), "정문", 4, null), null, null);
		PostCreateRequest tooLate = new PostCreateRequest(PostCategory.MEETUP, "먼 번개", "본문", null, false, null,
				new PostCreateRequest.MeetupRequest(LocalDateTime.now().plusDays(15), "정문", 4, null), null, null);

		assertError(() -> postService.create(me, soon), CommunityErrorCode.INVALID_MEETUP_TIME);
		assertError(() -> postService.create(me, tooLate), CommunityErrorCode.INVALID_MEETUP_TIME);
		assertError(() -> postService.create(me, TestPosts.meetup("번개", 4, List.of(me))), CommunityErrorCode.INVALID_COMPANIONS);
		assertError(() -> postService.create(me, TestPosts.meetup("번개", 2, List.of(kim, lee))), CommunityErrorCode.INVALID_COMPANIONS);
		assertError(() -> postService.create(me, TestPosts.meetup("번개", 4, List.of(kim, kim))), CommunityErrorCode.INVALID_COMPANIONS);
		assertError(() -> postService.create(me, TestPosts.meetup("번개", 4, List.of(pending))), AlumniErrorCode.USER_NOT_FOUND);
	}

	@Test
	void 커피챗_모집은_오늘_여러_명_신청을_받는_선배에게만_만들_수_있다() {
		Long senior = TestUsers.approved(entityManager, "선배사자");
		Long noSetting = TestUsers.approved(entityManager, "설정없는사자");
		Long oneOnOne = TestUsers.approved(entityManager, "일대일사자");
		TestProfiles.coffeeSetting(entityManager, oneOnOne, ReceiveMode.ON, false);
		Long resting = TestUsers.approved(entityManager, "쉬는사자");
		TestProfiles.coffeeSetting(entityManager, resting, ReceiveMode.ON, true);
		TestProfiles.coffeePeriod(entityManager, resting, LocalDate.now(), LocalDate.now().plusDays(1));

		for (Long notAllowed : List.of(noSetting, oneOnOne, resting)) {
			assertError(() -> postService.create(me, TestPosts.recruit("모집", notAllowed, 4)),
					CommunityErrorCode.GROUP_COFFEE_CHAT_NOT_ALLOWED);
		}
		assertError(() -> postService.create(me, TestPosts.recruit("모집", me, 4)), CommunityErrorCode.INVALID_RECRUIT_SENIOR);
		assertError(() -> postService.create(me, TestPosts.recruit("모집", 999_999L, 4)), AlumniErrorCode.USER_NOT_FOUND);

		TestPosts.acceptGroupCoffeeChat(entityManager, senior);
		Long postId = postService.create(me, TestPosts.recruit("모집", senior, 4)).id();
		assertThat(recruitMemberRepository.countByRecruitPostId(postId)).isEqualTo(1);
	}

	@Test
	void 작성자만_보낸_필드를_수정한다() {
		Long postId = postService.create(me, new PostCreateRequest(PostCategory.CONTEST, "공모전", "본문입니다.",
				List.of("https://a.com"), false, LocalDate.now().plusDays(3), null, null, null)).id();
		Long other = TestUsers.approved(entityManager, "김사자");
		PostUpdateRequest titleOnly = new PostUpdateRequest(null, "바뀐 제목", null, null, null, null, null, null);

		assertError(() -> postService.update(other, postId, titleOnly), CommunityErrorCode.NOT_OWNER);

		postService.update(me, postId, titleOnly);
		entityManager.flush();
		entityManager.clear();
		Post post = postRepository.findById(postId).orElseThrow();
		assertThat(post.getTitle()).isEqualTo("바뀐 제목");
		assertThat(post.getContent()).isEqualTo("본문입니다.");
		assertThat(post.getLinks()).containsExactly("https://a.com");
		assertThat(post.getDeadline()).isEqualTo(LocalDate.now().plusDays(3));
		assertThat(post.getUpdatedAt()).isNotNull();
	}

	@Test
	void 일반_카테고리끼리_바꿀_수_있고_마감일은_공모전_채용에만_남는다() {
		Long postId = postService.create(me, new PostCreateRequest(PostCategory.CONTEST, "공모전", "본문입니다.",
				null, false, LocalDate.now().plusDays(3), null, null, null)).id();
		Long anonymousId = postService.create(me, new PostCreateRequest(PostCategory.FREE, "익명", "본문입니다.",
				null, true, null, null, null, null)).id();

		postService.update(me, postId, new PostUpdateRequest(PostCategory.MATERIAL, null, null, null, null, null, null, null));
		entityManager.flush();
		entityManager.clear();
		assertThat(postRepository.findById(postId).orElseThrow().getDeadline()).isNull();

		assertError(() -> postService.update(me, postId, new PostUpdateRequest(PostCategory.MEETUP, null, null, null, null, null, null, null)),
				CommunityErrorCode.INVALID_CATEGORY_DETAIL);
		assertError(() -> postService.update(me, anonymousId, new PostUpdateRequest(PostCategory.CONTEST, null, null, null, null, null, null, null)),
				CommunityErrorCode.ANONYMOUS_NOT_ALLOWED);
	}

	@Test
	void 번개는_참여_인원보다_정원을_줄이거나_종료된_뒤에는_수정할_수_없다() {
		Long kim = TestUsers.approved(entityManager, "김사자");
		Long lee = TestUsers.approved(entityManager, "이사자");
		Long postId = postService.create(me, TestPosts.meetup("번개", 6, List.of(kim, lee))).id();

		assertError(() -> postService.update(me, postId, meetupUpdate(null, 2)), CommunityErrorCode.CAPACITY_BELOW_MEMBERS);

		LocalDateTime later = LocalDateTime.now().plusDays(3).withNano(0);
		postService.update(me, postId, meetupUpdate(later, 3));
		entityManager.flush();
		entityManager.clear();
		Meetup meetup = meetupRepository.findById(postId).orElseThrow();
		assertThat(meetup.getCapacity()).isEqualTo(3);
		assertThat(meetup.getMeetAt()).isEqualTo(later);

		TestPosts.update(entityManager, "update meetups set meet_at = dateadd('HOUR', -1, current_timestamp) where post_id = :id", postId);
		assertError(() -> postService.update(me, postId, new PostUpdateRequest(null, "제목 수정", null, null, null, null, null, null)),
				CommunityErrorCode.MEETUP_CLOSED);
	}

	@Test
	void 커피챗_신청_후나_참여자가_있으면_모집_정보를_바꿀_수_없다() {
		Long senior = TestUsers.approved(entityManager, "선배사자");
		Long otherSenior = TestUsers.approved(entityManager, "다른선배");
		TestPosts.acceptGroupCoffeeChat(entityManager, senior);
		TestPosts.acceptGroupCoffeeChat(entityManager, otherSenior);
		Long kim = TestUsers.approved(entityManager, "김사자");
		Long postId = postService.create(me, TestPosts.recruit("모집", senior, 4)).id();
		entityManager.createNativeQuery("insert into recruit_members (recruit_id, user_id, joined_at) values (:id, :userId, current_timestamp)")
				.setParameter("id", postId)
				.setParameter("userId", kim)
				.executeUpdate();

		PostUpdateRequest changeSenior = new PostUpdateRequest(null, null, null, null, null, null,
				new PostUpdateRequest.RecruitRequest(otherSenior, null, null), null);
		assertError(() -> postService.update(me, postId, changeSenior), CommunityErrorCode.RECRUIT_SENIOR_LOCKED);

		TestPosts.update(entityManager, "update coffeechat_recruits set status = 'APPLIED' where post_id = :id", postId);
		PostUpdateRequest changeCapacity = new PostUpdateRequest(null, null, null, null, null, null,
				new PostUpdateRequest.RecruitRequest(null, 5, null), null);
		assertError(() -> postService.update(me, postId, changeCapacity), CommunityErrorCode.RECRUIT_CLOSED);
	}

	@Test
	void 작성자나_관리자만_삭제하고_삭제한_글은_찾을_수_없다() {
		Long postId = postService.create(me, TestPosts.general(PostCategory.FREE, "자유글")).id();
		Long other = TestUsers.approved(entityManager, "김사자");
		Long admin = TestUsers.approved(entityManager, "관리자");
		entityManager.createNativeQuery("update users set role = 'ADMIN' where id = :id").setParameter("id", admin).executeUpdate();

		assertError(() -> postService.delete(other, postId), CommunityErrorCode.NOT_OWNER);

		postService.delete(admin, postId);
		assertThat(postRepository.findByIdAndDeletedAtIsNull(postId)).isEmpty();
		assertError(() -> postService.delete(me, postId), CommunityErrorCode.POST_NOT_FOUND);
	}

	@Test
	void 번개_글을_삭제하면_번개도_취소된다() {
		Long postId = postService.create(me, TestPosts.meetup("번개", 4, null)).id();

		postService.delete(me, postId);
		entityManager.flush();
		entityManager.clear();

		assertThat(meetupRepository.findById(postId).orElseThrow().isCanceled()).isTrue();
	}

	@Test
	void 커피챗을_신청한_모집_글은_작성자가_삭제할_수_없다() {
		Long senior = TestUsers.approved(entityManager, "선배사자");
		TestPosts.acceptGroupCoffeeChat(entityManager, senior);
		Long postId = postService.create(me, TestPosts.recruit("모집", senior, 4)).id();
		TestPosts.update(entityManager, "update coffeechat_recruits set status = 'APPLIED' where post_id = :id", postId);

		assertError(() -> postService.delete(me, postId), CommunityErrorCode.RECRUIT_CLOSED);
	}

	@Test
	void 일반_글을_번개나_커피챗_모집으로_바꾸면_모집_정보를_새로_만든다() {
		Long senior = TestUsers.approved(entityManager, "선배사자");
		TestPosts.acceptGroupCoffeeChat(entityManager, senior);
		Long toMeetup = postService.create(me, TestPosts.general(PostCategory.FREE, "번개로 바꿀 글")).id();
		Long toRecruit = postService.create(me, TestPosts.general(PostCategory.NOW, "모집으로 바꿀 글")).id();
		LocalDateTime meetAt = LocalDateTime.now().plusDays(2);

		postService.update(me, toMeetup, new PostUpdateRequest(PostCategory.MEETUP, null, null, null, null,
				new PostUpdateRequest.MeetupRequest(meetAt, "정문", 4), null, null));
		postService.update(me, toRecruit, new PostUpdateRequest(PostCategory.RECRUIT, null, null, null, null, null,
				new PostUpdateRequest.RecruitRequest(senior, 3, LocalDate.now().plusDays(3)), null));
		entityManager.flush();
		entityManager.clear();

		assertThat(postRepository.findById(toMeetup).orElseThrow().getCategory()).isEqualTo(PostCategory.MEETUP);
		assertThat(meetupRepository.findById(toMeetup).orElseThrow().getCapacity()).isEqualTo(4);
		assertThat(meetupMemberRepository.countByMeetupPostId(toMeetup)).isEqualTo(1);
		assertThat(postRepository.findById(toRecruit).orElseThrow().getCategory()).isEqualTo(PostCategory.RECRUIT);
		assertThat(coffeeChatRecruitRepository.findById(toRecruit).orElseThrow().getSeniorId()).isEqualTo(senior);
		assertThat(recruitMemberRepository.countByRecruitPostId(toRecruit)).isEqualTo(1);

		Long noInfo = postService.create(me, TestPosts.general(PostCategory.FREE, "정보 없는 글")).id();
		assertError(() -> postService.update(me, noInfo, new PostUpdateRequest(PostCategory.RECRUIT, null, null, null, null, null,
				new PostUpdateRequest.RecruitRequest(senior, null, null), null)), CommunityErrorCode.INVALID_CATEGORY_DETAIL);
		Long oneOnOne = TestUsers.approved(entityManager, "일대일사자");
		assertError(() -> postService.update(me, noInfo, new PostUpdateRequest(PostCategory.RECRUIT, null, null, null, null, null,
				new PostUpdateRequest.RecruitRequest(oneOnOne, 3, LocalDate.now().plusDays(3)), null)), CommunityErrorCode.GROUP_COFFEE_CHAT_NOT_ALLOWED);
	}

	@Test
	void 작성자_말고_참여자가_없으면_번개와_모집의_카테고리를_바꿀_수_있다() {
		Long kim = TestUsers.approved(entityManager, "김사자");
		Long senior = TestUsers.approved(entityManager, "선배사자");
		TestPosts.acceptGroupCoffeeChat(entityManager, senior);
		Long meetupId = postService.create(me, TestPosts.meetup("혼자 연 번개", 4, List.of())).id();
		meetupService.invite(me, meetupId, new MeetupInviteRequest(List.of(kim)));
		Long recruitId = postService.create(me, TestPosts.recruit("혼자 연 모집", senior, 3)).id();

		postService.update(me, meetupId, new PostUpdateRequest(PostCategory.FREE, null, null, null, null, null, null, null));
		postService.update(me, recruitId, new PostUpdateRequest(PostCategory.MEETUP, null, null, null, null,
				new PostUpdateRequest.MeetupRequest(LocalDateTime.now().plusDays(3), "후문", 5), null, null));
		entityManager.flush();
		entityManager.clear();

		assertThat(postRepository.findById(meetupId).orElseThrow().getCategory()).isEqualTo(PostCategory.FREE);
		assertThat(meetupRepository.findById(meetupId)).isEmpty();
		assertThat(meetupMemberRepository.countByMeetupPostId(meetupId)).isZero();
		assertThat(postRepository.findById(recruitId).orElseThrow().getCategory()).isEqualTo(PostCategory.MEETUP);
		assertThat(coffeeChatRecruitRepository.findById(recruitId)).isEmpty();
		assertThat(recruitMemberRepository.countByRecruitPostId(recruitId)).isZero();
		assertThat(meetupRepository.findById(recruitId).orElseThrow().getPlace()).isEqualTo("후문");
	}

	@Test
	void 참여자가_있거나_끝난_번개_모집은_카테고리를_바꿀_수_없다() {
		Long kim = TestUsers.approved(entityManager, "김사자");
		Long senior = TestUsers.approved(entityManager, "선배사자");
		TestPosts.acceptGroupCoffeeChat(entityManager, senior);
		Long meetupId = postService.create(me, TestPosts.meetup("함께하는 번개", 4, List.of(kim))).id();
		Long recruitId = postService.create(me, TestPosts.recruit("참여자 있는 모집", senior, 3)).id();
		recruitService.join(kim, recruitId);
		Long appliedId = postService.create(me, TestPosts.recruit("신청한 모집", senior, 3)).id();
		TestPosts.update(entityManager, "update coffeechat_recruits set status = 'APPLIED' where post_id = :id", appliedId);
		Long endedId = postService.create(me, TestPosts.meetup("끝난 번개", 4, List.of())).id();
		TestPosts.update(entityManager, "update meetups set meet_at = dateadd('HOUR', -1, current_timestamp) where post_id = :id", endedId);
		PostUpdateRequest toFree = new PostUpdateRequest(PostCategory.FREE, null, null, null, null, null, null, null);

		assertError(() -> postService.update(me, meetupId, toFree), CommunityErrorCode.CATEGORY_CHANGE_NOT_ALLOWED);
		assertError(() -> postService.update(me, recruitId, toFree), CommunityErrorCode.CATEGORY_CHANGE_NOT_ALLOWED);
		assertError(() -> postService.update(me, appliedId, toFree), CommunityErrorCode.RECRUIT_CLOSED);
		assertError(() -> postService.update(me, endedId, toFree), CommunityErrorCode.MEETUP_CLOSED);
	}

	@Test
	void 커피챗_모집을_취소하면_참여자에게_알림이_간다() {
		Long kim = TestUsers.approved(entityManager, "김사자");
		Long lee = TestUsers.approved(entityManager, "이사자");
		Long senior = TestUsers.approved(entityManager, "선배사자");
		TestPosts.acceptGroupCoffeeChat(entityManager, senior);
		Long postId = postService.create(me, TestPosts.recruit("같이 신청해요", senior, 4)).id();
		recruitService.join(kim, postId);
		recruitService.join(lee, postId);
		Long expiredId = postService.create(me, TestPosts.recruit("마감 지난 모집", senior, 4)).id();
		recruitService.join(kim, expiredId);
		TestPosts.update(entityManager,
				"update coffeechat_recruits set deadline = dateadd('DAY', -1, current_date) where post_id = :id", expiredId);

		postService.delete(me, postId);
		postService.delete(me, expiredId);

		List<Notification> notifications = notificationRepository.findAll().stream()
				.filter(notification -> notification.getType().equals("RECRUIT_CANCELED"))
				.toList();
		assertThat(notifications).extracting(Notification::getUserId).containsExactlyInAnyOrder(kim, lee);
		assertThat(notifications).extracting(Notification::getTargetId).containsOnly(postId);
		assertThat(notifications.get(0).getMessage()).isEqualTo("참여한 커피챗 모집이 취소됐어요. 같이 신청해요");
	}

	private PostUpdateRequest meetupUpdate(LocalDateTime meetAt, Integer capacity) {
		return new PostUpdateRequest(null, null, null, null, null,
				new PostUpdateRequest.MeetupRequest(meetAt, null, capacity), null, null);
	}

	private void assertError(ThrowingCallable callable, ErrorCode errorCode) {
		assertThatThrownBy(callable)
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(errorCode);
	}
}
