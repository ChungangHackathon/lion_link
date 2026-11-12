package com.lionlink.backend.domain.community.meetup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionlink.backend.domain.community.exception.CommunityErrorCode;
import com.lionlink.backend.domain.community.meetup.dto.request.MeetupInviteRequest;
import com.lionlink.backend.domain.community.meetup.dto.response.MeetupInviteResponse;
import com.lionlink.backend.domain.community.meetup.dto.response.MeetupParticipationResponse;
import com.lionlink.backend.domain.community.meetup.entity.MeetupStatus;
import com.lionlink.backend.domain.community.post.dto.response.PostMemberResponse;
import com.lionlink.backend.domain.community.post.entity.PostCategory;
import com.lionlink.backend.domain.community.post.service.PostService;
import com.lionlink.backend.domain.notification.entity.Notification;
import com.lionlink.backend.domain.notification.entity.NotificationTab;
import com.lionlink.backend.domain.notification.entity.NotificationTargetType;
import com.lionlink.backend.domain.notification.repository.NotificationRepository;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.ErrorCode;
import com.lionlink.backend.support.TestPosts;
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
class MeetupServiceTest {

	@Autowired
	private MeetupService meetupService;

	@Autowired
	private PostService postService;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private EntityManager entityManager;

	private Long host;
	private Long kim;
	private Long postId;

	@BeforeEach
	void setUp() {
		host = TestUsers.approved(entityManager, "나사자");
		kim = TestUsers.approved(entityManager, "김사자");
		postId = postService.create(host, TestPosts.meetup("금요일 번개", 3, List.of())).id();
	}

	@Test
	void 번개에_참여하면_참여자_목록과_상태를_돌려준다() {
		MeetupParticipationResponse response = meetupService.join(kim, postId);

		assertThat(response.joined()).isTrue();
		assertThat(response.participantCount()).isEqualTo(2);
		assertThat(response.capacity()).isEqualTo(3);
		assertThat(response.status()).isEqualTo(MeetupStatus.OPEN);
		assertThat(response.members()).extracting(PostMemberResponse::id).containsExactly(host, kim);
		assertThat(response.members()).extracting(PostMemberResponse::isHost).containsExactly(true, false);
	}

	@Test
	void 이미_참여_중이면_그대로_성공한다() {
		meetupService.join(kim, postId);

		MeetupParticipationResponse again = meetupService.join(kim, postId);

		assertThat(again.joined()).isTrue();
		assertThat(again.participantCount()).isEqualTo(2);
	}

	@Test
	void 정원이_차면_참여할_수_없다() {
		Long lee = TestUsers.approved(entityManager, "이사자");
		Long park = TestUsers.approved(entityManager, "박사자");
		MeetupParticipationResponse full = meetupService.join(lee, postId);
		meetupService.join(kim, postId);

		assertThat(full.status()).isEqualTo(MeetupStatus.OPEN);
		assertThat(meetupService.join(kim, postId).status()).isEqualTo(MeetupStatus.FULL);
		assertError(() -> meetupService.join(park, postId), CommunityErrorCode.MEETUP_FULL);
	}

	@Test
	void 시작되거나_취소된_번개에는_참여할_수_없다() {
		Long canceledId = postService.create(host, TestPosts.meetup("취소 번개", 4, List.of())).id();
		TestPosts.update(entityManager, "update meetups set canceled_at = current_timestamp where post_id = :id", canceledId);
		TestPosts.update(entityManager, "update meetups set meet_at = dateadd('MINUTE', -1, current_timestamp) where post_id = :id", postId);

		assertError(() -> meetupService.join(kim, postId), CommunityErrorCode.MEETUP_CLOSED);
		assertError(() -> meetupService.join(kim, canceledId), CommunityErrorCode.MEETUP_CLOSED);
	}

	@Test
	void 번개가_아니거나_삭제된_게시글은_찾을_수_없다() {
		Long freeId = postService.create(host, TestPosts.general(PostCategory.FREE, "자유 글")).id();
		Long deletedId = postService.create(host, TestPosts.meetup("삭제 번개", 4, List.of())).id();
		postService.delete(host, deletedId);

		assertError(() -> meetupService.join(kim, freeId), CommunityErrorCode.MEETUP_NOT_FOUND);
		assertError(() -> meetupService.join(kim, deletedId), CommunityErrorCode.POST_NOT_FOUND);
	}

	@Test
	void 참여를_취소하면_빈자리에_다시_참여할_수_있다() {
		meetupService.join(kim, postId);

		MeetupParticipationResponse left = meetupService.leave(kim, postId);

		assertThat(left.joined()).isFalse();
		assertThat(left.participantCount()).isEqualTo(1);
		assertThat(left.members()).extracting(PostMemberResponse::id).containsExactly(host);
		assertThat(meetupService.join(kim, postId).joined()).isTrue();
	}

	@Test
	void 작성자나_참여하지_않은_회원은_참여를_취소할_수_없다() {
		assertError(() -> meetupService.leave(host, postId), CommunityErrorCode.HOST_CANNOT_LEAVE);
		assertError(() -> meetupService.leave(kim, postId), CommunityErrorCode.NOT_MEETUP_MEMBER);
	}

	@Test
	void 시작된_번개는_참여를_취소할_수_없다() {
		meetupService.join(kim, postId);
		TestPosts.update(entityManager, "update meetups set meet_at = dateadd('MINUTE', -1, current_timestamp) where post_id = :id", postId);

		assertError(() -> meetupService.leave(kim, postId), CommunityErrorCode.MEETUP_CLOSED);
	}

	@Test
	void 초대는_참여자_이미_초대한_회원_승인되지_않은_회원을_건너뛰고_알림을_저장한다() {
		Long lee = TestUsers.approved(entityManager, "이사자");
		Long park = TestUsers.approved(entityManager, "박사자");
		Long pending = TestUsers.create(entityManager, "대기사자", UserStatus.PENDING);
		meetupService.join(kim, postId);
		meetupService.invite(host, postId, new MeetupInviteRequest(List.of(park)));

		MeetupInviteResponse response = meetupService.invite(host, postId,
				new MeetupInviteRequest(List.of(lee, lee, kim, park, pending, host, 999_999L)));

		assertThat(response.sentCount()).isEqualTo(1);
		assertThat(response.skippedUserIds()).containsExactly(kim, park, pending, host, 999_999L);
		List<Notification> notifications = invitedNotifications();
		assertThat(notifications).extracting(Notification::getUserId).containsExactlyInAnyOrder(park, lee);
		Notification notification = notifications.stream().filter(n -> n.getUserId().equals(lee)).findFirst().orElseThrow();
		assertThat(notification.getTab()).isEqualTo(NotificationTab.COMMUNITY);
		assertThat(notification.getTargetType()).isEqualTo(NotificationTargetType.POST);
		assertThat(notification.getTargetId()).isEqualTo(postId);
		assertThat(notification.getMessage()).isEqualTo("나사자님이 번개에 초대했어요. 금요일 번개");
	}

	@Test
	void 작성자만_진행_중인_번개에_초대할_수_있다() {
		Long lee = TestUsers.approved(entityManager, "이사자");
		MeetupInviteRequest request = new MeetupInviteRequest(List.of(lee));

		assertError(() -> meetupService.invite(kim, postId, request), CommunityErrorCode.NOT_OWNER);

		TestPosts.update(entityManager, "update meetups set meet_at = dateadd('MINUTE', -1, current_timestamp) where post_id = :id", postId);
		assertError(() -> meetupService.invite(host, postId, request), CommunityErrorCode.MEETUP_CLOSED);
	}

	@Test
	void 번개_글을_삭제하면_작성자를_뺀_참여자에게_취소_알림을_저장한다() {
		Long lee = TestUsers.approved(entityManager, "이사자");
		meetupService.join(kim, postId);
		meetupService.join(lee, postId);

		postService.delete(host, postId);
		entityManager.flush();

		List<Notification> notifications = notificationRepository.findAll().stream()
				.filter(n -> n.getType().equals("MEETUP_CANCELED"))
				.toList();
		assertThat(notifications).extracting(Notification::getUserId).containsExactlyInAnyOrder(kim, lee);
		assertThat(notifications.get(0).getMessage()).isEqualTo("참여한 번개가 취소됐어요. 금요일 번개");
	}

	private List<Notification> invitedNotifications() {
		entityManager.flush();
		return notificationRepository.findAll().stream()
				.filter(n -> n.getType().equals("MEETUP_INVITED"))
				.toList();
	}

	private void assertError(ThrowingCallable callable, ErrorCode errorCode) {
		assertThatThrownBy(callable)
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(errorCode);
	}
}
