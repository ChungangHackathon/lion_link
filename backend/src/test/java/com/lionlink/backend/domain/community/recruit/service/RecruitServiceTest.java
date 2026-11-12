package com.lionlink.backend.domain.community.recruit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatStatus;
import com.lionlink.backend.domain.community.exception.CommunityErrorCode;
import com.lionlink.backend.domain.community.post.dto.response.PostMemberResponse;
import com.lionlink.backend.domain.community.post.entity.PostCategory;
import com.lionlink.backend.domain.community.post.service.PostService;
import com.lionlink.backend.domain.community.recruit.dto.response.RecruitParticipationResponse;
import com.lionlink.backend.domain.community.recruit.entity.RecruitStatus;
import com.lionlink.backend.domain.notification.entity.Notification;
import com.lionlink.backend.domain.notification.entity.NotificationTab;
import com.lionlink.backend.domain.notification.entity.NotificationTargetType;
import com.lionlink.backend.domain.notification.repository.NotificationRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.ErrorCode;
import com.lionlink.backend.support.TestPosts;
import com.lionlink.backend.support.TestProfiles;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RecruitServiceTest {

	@Autowired
	private RecruitService recruitService;

	@Autowired
	private PostService postService;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private EntityManager entityManager;

	private Long me;
	private Long senior;
	private Long kim;
	private Long postId;

	@BeforeEach
	void setUp() {
		me = TestUsers.approved(entityManager, "나사자");
		senior = TestUsers.approved(entityManager, "선배사자");
		kim = TestUsers.approved(entityManager, "김사자");
		TestPosts.acceptGroupCoffeeChat(entityManager, senior);
		postId = postService.create(me, TestPosts.recruit("함께 커피챗 신청해요", senior, 3)).id();
	}

	@Test
	void 참여하면_참여자가_되고_작성자에게_알림이_간다() {
		RecruitParticipationResponse result = recruitService.join(kim, postId);

		assertThat(result.postId()).isEqualTo(postId);
		assertThat(result.joined()).isTrue();
		assertThat(result.memberCount()).isEqualTo(2);
		assertThat(result.capacity()).isEqualTo(3);
		assertThat(result.status()).isEqualTo(RecruitStatus.OPEN);
		assertThat(result.members()).extracting(PostMemberResponse::id).containsExactly(me, kim);
		assertThat(result.members()).extracting(PostMemberResponse::isHost).containsExactly(true, false);

		List<Notification> notifications = joinedNotifications();
		assertThat(notifications).hasSize(1);
		Notification notification = notifications.get(0);
		assertThat(notification.getUserId()).isEqualTo(me);
		assertThat(notification.getTab()).isEqualTo(NotificationTab.COMMUNITY);
		assertThat(notification.getTargetType()).isEqualTo(NotificationTargetType.POST);
		assertThat(notification.getTargetId()).isEqualTo(postId);
		assertThat(notification.getMessage()).isEqualTo("김사자님이 내 모집글에 참여했어요. 함께 커피챗 신청해요");
	}

	@Test
	void 이미_참여_중이면_그대로_성공하고_알림을_다시_보내지_않는다() {
		recruitService.join(kim, postId);

		RecruitParticipationResponse again = recruitService.join(kim, postId);
		RecruitParticipationResponse author = recruitService.join(me, postId);

		assertThat(again.memberCount()).isEqualTo(2);
		assertThat(author.joined()).isTrue();
		assertThat(author.memberCount()).isEqualTo(2);
		assertThat(joinedNotifications()).hasSize(1);
	}

	@Test
	void 신청받을_사자_본인은_참여할_수_없다() {
		assertError(() -> recruitService.join(senior, postId), CommunityErrorCode.SENIOR_CANNOT_JOIN);
	}

	@Test
	void 없거나_삭제된_글이거나_커피챗_모집_글이_아니면_404() {
		Long general = postService.create(me, TestPosts.general(PostCategory.FREE, "자유 글")).id();
		Long deleted = postService.create(me, TestPosts.recruit("삭제할 모집", senior, 3)).id();
		postService.delete(me, deleted);

		assertError(() -> recruitService.join(kim, general), CommunityErrorCode.RECRUIT_NOT_FOUND);
		assertError(() -> recruitService.join(kim, deleted), CommunityErrorCode.POST_NOT_FOUND);
		assertError(() -> recruitService.join(kim, 999_999L), CommunityErrorCode.POST_NOT_FOUND);
	}

	@Test
	void 마감일이_지났거나_신청이_끝난_모집은_참여할_수_없다() {
		TestPosts.update(entityManager, "update coffeechat_recruits set deadline = current_date where post_id = :id", postId);
		assertThat(recruitService.join(kim, postId).joined()).isTrue();

		Long lee = TestUsers.approved(entityManager, "이사자");
		TestPosts.update(entityManager,
				"update coffeechat_recruits set deadline = dateadd('DAY', -1, current_date) where post_id = :id", postId);
		assertError(() -> recruitService.join(lee, postId), CommunityErrorCode.RECRUIT_CLOSED);

		Long applied = postService.create(me, TestPosts.recruit("신청한 모집", senior, 3)).id();
		TestPosts.update(entityManager, "update coffeechat_recruits set status = 'APPLIED' where post_id = :id", applied);
		assertError(() -> recruitService.join(lee, applied), CommunityErrorCode.RECRUIT_CLOSED);
	}

	@Test
	void 정원이_차면_참여할_수_없다() {
		recruitService.join(kim, postId);
		Long lee = TestUsers.approved(entityManager, "이사자");
		recruitService.join(lee, postId);
		Long park = TestUsers.approved(entityManager, "박사자");

		assertError(() -> recruitService.join(park, postId), CommunityErrorCode.RECRUIT_FULL);
	}

	@Test
	void 그_사자와_진행_중인_커피챗이_있으면_참여할_수_없다() {
		TestProfiles.coffeeChat(entityManager, kim, senior, CoffeeChatStatus.PENDING);
		Long lee = TestUsers.approved(entityManager, "이사자");
		TestProfiles.coffeeChat(entityManager, lee, senior, CoffeeChatStatus.COMPLETED);

		assertError(() -> recruitService.join(kim, postId), CommunityErrorCode.OPEN_COFFEE_CHAT_EXISTS);
		assertThat(recruitService.join(lee, postId).joined()).isTrue();
	}

	private List<Notification> joinedNotifications() {
		return notificationRepository.findAll().stream()
				.filter(notification -> notification.getType().equals("RECRUIT_JOINED"))
				.toList();
	}

	private void assertError(ThrowingCallable callable, ErrorCode errorCode) {
		assertThatThrownBy(callable)
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(errorCode);
	}
}
