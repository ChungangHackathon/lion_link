package com.lionlink.backend.domain.community.recruit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.lionlink.backend.domain.alumni.dto.response.AlumniDetailResponse.CoffeeChatAvailability;
import com.lionlink.backend.domain.alumni.service.AlumniDetailService;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatCreateRequest;
import com.lionlink.backend.domain.coffeechat.entity.MeetingMode;
import com.lionlink.backend.domain.coffeechat.entity.PreferredMode;
import com.lionlink.backend.domain.coffeechat.entity.ReceiveMode;
import com.lionlink.backend.domain.coffeechat.exception.CoffeeChatErrorCode;
import com.lionlink.backend.domain.coffeechat.service.CoffeeChatService;
import com.lionlink.backend.domain.community.exception.CommunityErrorCode;
import com.lionlink.backend.domain.community.post.dto.request.PostUpdateRequest;
import com.lionlink.backend.domain.community.post.service.PostService;
import com.lionlink.backend.domain.community.recruit.entity.CoffeeChatRecruit;
import com.lionlink.backend.domain.community.recruit.entity.RecruitStatus;
import com.lionlink.backend.domain.notification.entity.Notification;
import com.lionlink.backend.domain.notification.repository.NotificationRepository;
import com.lionlink.backend.domain.profile.coffee.CoffeeTopic;
import com.lionlink.backend.domain.profile.coffee.dto.request.CoffeeSettingUpdateRequest;
import com.lionlink.backend.domain.profile.coffee.dto.request.CoffeeSwitchRequest;
import com.lionlink.backend.domain.profile.coffee.dto.request.CoffeeSwitchRequest.RestFor;
import com.lionlink.backend.domain.profile.coffee.service.CoffeeSettingService;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.ErrorCode;
import com.lionlink.backend.support.TestPosts;
import com.lionlink.backend.support.TestProfiles;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CoffeeSettingChangeTest {

	@Autowired
	private CoffeeSettingService coffeeSettingService;

	@Autowired
	private PostService postService;

	@Autowired
	private RecruitService recruitService;

	@Autowired
	private CoffeeChatService coffeeChatService;

	@Autowired
	private AlumniDetailService alumniDetailService;

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
		me = alumni("나사자");
		senior = alumni("선배사자");
		kim = alumni("김사자");
		TestPosts.acceptGroupCoffeeChat(entityManager, senior);
		postId = postService.create(me, TestPosts.recruit("함께 커피챗 신청해요", senior, 3)).id();
		recruitService.join(kim, postId);
	}

	@Test
	void 여러_명_커피챗을_끄면_그_사자의_모집글이_마감되고_참여자에게_알림이_간다() {
		Long other = alumni("다른선배사자");
		TestPosts.acceptGroupCoffeeChat(entityManager, other);
		Long otherPostId = postService.create(me, TestPosts.recruit("다른 모집", other, 3)).id();

		updateSetting(senior, false);
		updateSetting(senior, false);

		assertThat(recruit(postId).getStatus()).isEqualTo(RecruitStatus.ENDED);
		assertThat(recruit(otherPostId).getStatus()).isEqualTo(RecruitStatus.OPEN);
		assertThat(closedNotifications()).extracting(Notification::getUserId, Notification::getActorId, Notification::getTargetId)
				.containsExactlyInAnyOrder(tuple(me, senior, postId), tuple(kim, senior, postId));
		assertThat(closedNotifications().get(0).getMessage()).isEqualTo("님이 여러 명 커피챗을 받지 않게 되어 모집이 마감됐어요. 함께 커피챗 신청해요");
	}

	@Test
	void 마감된_모집은_참여와_수정이_안_되고_작성자는_글을_지울_수_있다() {
		updateSetting(senior, false);
		Long lee = alumni("이사자");

		assertError(() -> recruitService.join(lee, postId), CommunityErrorCode.RECRUIT_CLOSED);
		assertThatThrownBy(() -> postService.update(me, postId, new PostUpdateRequest(null, null, null, null, null, null,
				new PostUpdateRequest.RecruitRequest(null, 4, null), null)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("마감된 모집은 바꿀 수 없어요.");
		assertError(() -> coffeeChatService.create(me, fromRecruit()), CommunityErrorCode.RECRUIT_CLOSED);

		postService.delete(me, postId);
		entityManager.flush();
		assertThat(notificationRepository.findAll()).noneMatch(notification -> notification.getType().equals("RECRUIT_CANCELED"));
	}

	@Test
	void 여러_명_커피챗을_끄면_동문_상세와_신청에_바로_반영된다() {
		updateSetting(senior, false);

		assertThat(alumniDetailService.getDetail(kim, senior).coffeeChat().acceptGroup()).isFalse();
		assertError(() -> coffeeChatService.create(kim, request(List.of(me))), CoffeeChatErrorCode.GROUP_COFFEE_CHAT_NOT_ALLOWED);
		assertThat(coffeeChatService.create(kim, request(List.of())).id()).isNotNull();
	}

	@Test
	void 받기를_끄면_모집글은_그대로고_신청은_막힌다() {
		coffeeSettingService.switchReceiving(senior, new CoffeeSwitchRequest(false, RestFor.UNTIL_RESUME, null, null, null));

		assertThat(recruit(postId).statusOn(LocalDate.now())).isEqualTo(RecruitStatus.OPEN);
		assertThat(alumniDetailService.getDetail(kim, senior).coffeeChat().availability()).isEqualTo(CoffeeChatAvailability.NOT_ACCEPTING);
		assertError(() -> coffeeChatService.create(me, fromRecruit()), CoffeeChatErrorCode.NOT_ACCEPTING);
		assertThat(closedNotifications()).isEmpty();

		coffeeSettingService.switchReceiving(senior, new CoffeeSwitchRequest(true, null, null, null, null));
		assertThat(alumniDetailService.getDetail(kim, senior).coffeeChat().availability()).isEqualTo(CoffeeChatAvailability.AVAILABLE);
	}

	private void updateSetting(Long userId, boolean acceptGroup) {
		coffeeSettingService.update(userId, new CoffeeSettingUpdateRequest(ReceiveMode.ON, acceptGroup, List.of(CoffeeTopic.CAREER),
				List.of(MeetingMode.ONLINE), null, null, null));
		entityManager.flush();
		entityManager.clear();
	}

	private CoffeeChatCreateRequest request(List<Long> memberIds) {
		return new CoffeeChatCreateRequest(senior, "CAREER", null, "선배님 이야기를 듣고 싶어요.", null, PreferredMode.ONLINE,
				null, memberIds, null);
	}

	private CoffeeChatCreateRequest fromRecruit() {
		return new CoffeeChatCreateRequest(null, "CAREER", null, "선배님 이야기를 듣고 싶어요.", null, PreferredMode.ONLINE,
				null, null, postId);
	}

	private CoffeeChatRecruit recruit(Long id) {
		entityManager.flush();
		entityManager.clear();
		return entityManager.find(CoffeeChatRecruit.class, id);
	}

	private List<Notification> closedNotifications() {
		entityManager.flush();
		return notificationRepository.findAll().stream()
				.filter(notification -> notification.getType().equals("RECRUIT_CLOSED"))
				.toList();
	}

	private Long alumni(String name) {
		Long userId = TestUsers.approved(entityManager, name);
		TestProfiles.save(entityManager, TestProfiles.profile(userId));
		return userId;
	}

	private void assertError(ThrowingCallable callable, ErrorCode errorCode) {
		assertThatThrownBy(callable)
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(errorCode);
	}
}
