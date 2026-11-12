package com.lionlink.backend.domain.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.lionlink.backend.domain.alumni.dto.response.AlumniResponse;
import com.lionlink.backend.domain.alumni.service.BookmarkService;
import com.lionlink.backend.domain.chat.entity.ChatMessage;
import com.lionlink.backend.domain.chat.entity.ChatMessageType;
import com.lionlink.backend.domain.chat.entity.ChatRoom;
import com.lionlink.backend.domain.chat.entity.ChatRoomMember;
import com.lionlink.backend.domain.chat.entity.ChatRoomStatus;
import com.lionlink.backend.domain.chat.entity.ChatRoomType;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChat;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatMember;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatMemberRole;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatStatus;
import com.lionlink.backend.domain.coffeechat.entity.MeetingMode;
import com.lionlink.backend.domain.community.post.entity.PostCategory;
import com.lionlink.backend.domain.community.post.service.PostService;
import com.lionlink.backend.domain.home.dto.response.GuideDismissResponse;
import com.lionlink.backend.domain.home.dto.response.HomeResponse;
import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.support.TestPosts;
import com.lionlink.backend.support.TestProfiles;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class HomeServiceTest {

	@Autowired
	private HomeService homeService;

	@Autowired
	private BookmarkService bookmarkService;

	@Autowired
	private PostService postService;

	@Autowired
	private EntityManager entityManager;

	private Long me;

	@BeforeEach
	void setUp() {
		me = user("나사자", 14);
		TestProfiles.save(entityManager, TestProfiles.profile(me).headline("백엔드 지망생").bio("반가워요"));
	}

	@Test
	void 내_정보와_환영_배너_표시_여부를_돌려준다() {
		HomeResponse home = homeService.getHome(me);

		assertThat(home.me().name()).isEqualTo("나사자");
		assertThat(home.me().generation()).isEqualTo(14);
		assertThat(home.me().lionParts()).containsExactly(LionPart.BACKEND);
		assertThat(home.me().headline()).isEqualTo("백엔드 지망생");
		assertThat(home.me().bio()).isEqualTo("반가워요");
		assertThat(home.welcomeBannerVisible()).isTrue();
		assertThat(home.upcomingCoffeeChat()).isNull();
		assertThat(home.onThisDay()).isNull();
	}

	@Test
	void 환영_배너를_닫으면_처음_닫은_시각을_유지한다() {
		GuideDismissResponse first = homeService.dismissGuide(me);
		entityManager.flush();
		entityManager.clear();
		GuideDismissResponse second = homeService.dismissGuide(me);

		assertThat(first.visible()).isFalse();
		assertThat(second.dismissedAt()).isCloseTo(first.dismissedAt(), within(1, ChronoUnit.MILLIS));
		assertThat(homeService.getHome(me).welcomeBannerVisible()).isFalse();
	}

	@Test
	void 받은_대기_커피챗과_진행_중인_커피챗과_관심_동문_수를_센다() {
		Long a = user("가사자", 14);
		Long b = user("나사자2", 14);
		Long c = user("다사자", 14);
		Long d = user("라사자", 14);
		Long host = user("모임장", 12);
		coffeeChat(a, me, CoffeeChatStatus.PENDING, null);
		coffeeChat(b, me, CoffeeChatStatus.PENDING, null);
		coffeeChat(me, c, CoffeeChatStatus.PENDING, null);
		coffeeChat(d, me, CoffeeChatStatus.ACCEPTED, null);
		coffeeChat(me, a, CoffeeChatStatus.SCHEDULED, LocalDateTime.now().plusDays(1));
		coffeeChat(me, b, CoffeeChatStatus.COMPLETED, null);
		Long group = coffeeChat(c, host, CoffeeChatStatus.SCHEDULED, LocalDateTime.now().plusDays(2));
		entityManager.persist(CoffeeChatMember.builder()
				.coffeeChat(entityManager.find(CoffeeChat.class, group))
				.userId(me)
				.role(CoffeeChatMemberRole.MEMBER)
				.build());
		bookmarkService.bookmark(me, a);
		entityManager.flush();

		HomeResponse.Counts counts = homeService.getHome(me).counts();

		assertThat(counts.pendingCoffeeChats()).isEqualTo(2);
		assertThat(counts.activeCoffeeChats()).isEqualTo(3);
		assertThat(counts.bookmarks()).isEqualTo(1);
	}

	@Test
	void 안_읽은_메시지와_받은_메시지_요청을_센다() {
		Long kim = user("김사자", 14);
		Long lee = user("이사자", 14);
		ChatRoom room = room(me, kim, ChatRoomStatus.ACTIVE, null);
		Long read = message(room, kim);
		message(room, kim);
		message(room, kim);
		message(room, me);
		update("update chat_room_members set last_read_message_id = " + read + " where room_id = " + room.getId() + " and user_id = " + me);
		ChatRoom left = room(me, lee, ChatRoomStatus.ACTIVE, null);
		message(left, lee);
		update("update chat_room_members set left_at = current_timestamp where room_id = " + left.getId() + " and user_id = " + me);
		Long park = user("박사자", 14);
		Long choi = user("최사자", 14);
		room(park, me, ChatRoomStatus.REQUESTED, park);
		room(me, choi, ChatRoomStatus.REQUESTED, me);

		HomeResponse.Counts counts = homeService.getHome(me).counts();

		assertThat(counts.unreadMessages()).isEqualTo(2);
		assertThat(counts.messageRequests()).isEqualTo(1);
	}

	@Test
	void 다가오는_커피챗은_가장_가까운_확정_일정_하나다() {
		Long senior = user("선배사자", 11);
		Long other = user("다른선배", 12);
		Long past = user("지난선배", 12);
		coffeeChat(me, other, CoffeeChatStatus.SCHEDULED, LocalDateTime.now().plusDays(5));
		Long soonest = coffeeChat(me, senior, CoffeeChatStatus.SCHEDULED, LocalDateTime.now().plusDays(1));
		coffeeChat(me, past, CoffeeChatStatus.SCHEDULED, LocalDateTime.now().minusDays(1));
		ChatRoom room = room(me, senior, ChatRoomStatus.ACTIVE, null);
		update("update chat_rooms set coffee_chat_id = " + soonest + " where id = " + room.getId());

		HomeResponse.UpcomingCoffeeChat upcoming = homeService.getHome(me).upcomingCoffeeChat();

		assertThat(upcoming.id()).isEqualTo(soonest);
		assertThat(upcoming.meetingMode()).isEqualTo(MeetingMode.ONLINE);
		assertThat(upcoming.roomId()).isEqualTo(room.getId());
		assertThat(upcoming.partner().name()).isEqualTo("선배사자");
		assertThat(upcoming.partner().generation()).isEqualTo(11);
	}

	@Test
	void 선배_추천은_커피챗을_받는_선배를_같은_파트부터_두_명_보여준다() {
		Long designSenior = senior("디자인선배", 12, LionPart.DESIGN, true);
		Long backendSenior = senior("백엔드선배", 11, LionPart.BACKEND, true);
		Long closestSenior = senior("가까운선배", 13, LionPart.FRONTEND, true);
		senior("안받는선배", 12, LionPart.BACKEND, false);
		senior("후배", 15, LionPart.BACKEND, true);
		Long noSetting = user("설정없는선배", 12);
		TestProfiles.save(entityManager, TestProfiles.profile(noSetting));

		List<AlumniResponse> seniors = homeService.getHome(me).recommendedSeniors();

		assertThat(seniors).extracting(AlumniResponse::id).containsExactly(backendSenior, closestSenior);
		assertThat(seniors).extracting(AlumniResponse::id).doesNotContain(designSenior);
	}

	@Test
	void 일년_전_오늘은_그날_쓴_글_중_조회수가_가장_많은_글이다() {
		Long popular = postService.create(me, TestPosts.general(PostCategory.NOW, "중커톤 시상식 후기")).id();
		Long quiet = postService.create(me, TestPosts.general(PostCategory.FREE, "조용한 글")).id();
		Long yesterday = postService.create(me, TestPosts.general(PostCategory.NOW, "하루 전 글")).id();
		Long deleted = postService.create(me, TestPosts.general(PostCategory.NOW, "삭제된 글")).id();
		update("update posts set created_at = dateadd('YEAR', -1, current_timestamp), view_count = 10 where id = " + popular);
		update("update posts set created_at = dateadd('YEAR', -1, current_timestamp), view_count = 1 where id = " + quiet);
		update("update posts set created_at = dateadd('DAY', -1, dateadd('YEAR', -1, current_timestamp)), view_count = 99 where id = " + yesterday);
		update("update posts set created_at = dateadd('YEAR', -1, current_timestamp), view_count = 50, deleted_at = current_timestamp where id = " + deleted);

		HomeResponse.OnThisDay onThisDay = homeService.getHome(me).onThisDay();

		assertThat(onThisDay.id()).isEqualTo(popular);
		assertThat(onThisDay.category()).isEqualTo(PostCategory.NOW);
		assertThat(onThisDay.title()).isEqualTo("중커톤 시상식 후기");
	}

	private Long user(String name, int generation) {
		Long userId = TestUsers.approved(entityManager, name);
		update("update users set generation = " + generation + " where id = " + userId);
		return userId;
	}

	private Long senior(String name, int generation, LionPart part, boolean receiving) {
		Long userId = user(name, generation);
		TestProfiles.save(entityManager, TestProfiles.profile(userId).lionPart1(part));
		TestPosts.acceptGroupCoffeeChat(entityManager, userId);
		if (!receiving) {
			update("update coffee_settings set default_mode = 'OFF' where user_id = " + userId);
		}
		return userId;
	}

	private Long coffeeChat(Long requesterId, Long receiverId, CoffeeChatStatus status, LocalDateTime scheduledAt) {
		CoffeeChat coffeeChat = CoffeeChat.builder()
				.requesterId(requesterId)
				.receiverId(receiverId)
				.topic("CAREER")
				.reason("이야기 나누고 싶어요.")
				.questions(List.of())
				.preferredMode(MeetingMode.ONLINE)
				.expiresAt(LocalDateTime.now().plusDays(7))
				.build();
		entityManager.persist(coffeeChat);
		entityManager.flush();
		entityManager.createNativeQuery("""
						update coffee_chats set status = :status, scheduled_at = :scheduledAt, meeting_mode = 'ONLINE', open_flag = null
						where id = :id
						""")
				.setParameter("status", status.name())
				.setParameter("scheduledAt", scheduledAt)
				.setParameter("id", coffeeChat.getId())
				.executeUpdate();
		entityManager.clear();
		return coffeeChat.getId();
	}

	private ChatRoom room(Long userA, Long userB, ChatRoomStatus status, Long requestedBy) {
		ChatRoom room = ChatRoom.builder()
				.type(ChatRoomType.DIRECT)
				.userAId(userA)
				.userBId(userB)
				.status(status)
				.requestedBy(requestedBy)
				.requestedAt(requestedBy == null ? null : LocalDateTime.now())
				.build();
		entityManager.persist(room);
		entityManager.persist(ChatRoomMember.builder().room(room).userId(userA).build());
		entityManager.persist(ChatRoomMember.builder().room(room).userId(userB).build());
		entityManager.flush();
		return room;
	}

	private Long message(ChatRoom room, Long senderId) {
		ChatMessage message = ChatMessage.builder()
				.room(room)
				.senderId(senderId)
				.type(ChatMessageType.TEXT)
				.content("안녕하세요")
				.clientMsgId(UUID.randomUUID().toString())
				.build();
		entityManager.persist(message);
		entityManager.flush();
		return message.getId();
	}

	private void update(String sql) {
		entityManager.flush();
		entityManager.createNativeQuery(sql).executeUpdate();
		entityManager.clear();
	}
}
