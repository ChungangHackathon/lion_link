package com.lionlink.backend.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lionlink.backend.domain.chat.dto.request.ChatMessageSendRequest;
import com.lionlink.backend.domain.chat.dto.request.ChatRequestCreateRequest;
import com.lionlink.backend.domain.chat.dto.response.ChatRoomDetailResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatAcceptRequest;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatCreateRequest;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatScheduleRequest;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeSetting;
import com.lionlink.backend.domain.coffeechat.entity.MeetingMode;
import com.lionlink.backend.domain.coffeechat.entity.PreferredMode;
import com.lionlink.backend.domain.coffeechat.service.CoffeeChatService;
import com.lionlink.backend.support.TestProfiles;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ChatRoomQueryTest {

	@Autowired
	private ChatRoomService chatRoomService;

	@Autowired
	private ChatRequestService chatRequestService;

	@Autowired
	private CoffeeChatService coffeeChatService;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	private Long me;

	@BeforeEach
	void setUp() {
		me = alumni("나사자");
	}

	@Test
	void 채팅방_정보에_커피챗_일정_후보와_질문과_만남_방식을_준다() {
		Long senior = alumni("선배사자");
		entityManager.persist(CoffeeSetting.builder().userId(senior).topics(List.of("CAREER")).modes(List.of(MeetingMode.ONLINE)).build());
		entityManager.flush();
		Long chatId = coffeeChatService.create(me, new CoffeeChatCreateRequest(senior, "CAREER", null, "선배님 이야기를 듣고 싶어요.",
				List.of("어떻게 공부하셨나요?", "첫 회사는 어떻게 고르셨나요?"), PreferredMode.ONLINE, null, List.of(), null)).id();
		LocalDateTime first = LocalDateTime.now().plusDays(2).withNano(0);
		LocalDateTime second = LocalDateTime.now().plusDays(3).withNano(0);
		Long roomId = coffeeChatService.accept(senior, chatId, new CoffeeChatAcceptRequest(List.of(first, second), null)).roomId();

		ChatRoomDetailResponse.CoffeeChat accepted = detail(roomId).coffeeChat();
		assertThat(accepted.id()).isEqualTo(chatId);
		assertThat(accepted.proposedSlots()).containsExactly(first, second);
		assertThat(accepted.questions()).containsExactly("어떻게 공부하셨나요?", "첫 회사는 어떻게 고르셨나요?");
		assertThat(accepted.scheduledAt()).isNull();
		assertThat(accepted.meetingMode()).isNull();

		coffeeChatService.schedule(me, chatId, new CoffeeChatScheduleRequest(first, MeetingMode.OFFLINE, "강남역 카페", null));
		ChatRoomDetailResponse.CoffeeChat scheduled = detail(roomId).coffeeChat();
		assertThat(scheduled.scheduledAt()).isEqualTo(first);
		assertThat(scheduled.meetingMode()).isEqualTo(MeetingMode.OFFLINE);
		assertThat(scheduled.meetingPlace()).isEqualTo("강남역 카페");
		assertThat(scheduled.meetingLink()).isNull();
	}

	@Test
	void 방이_늘어도_목록_조회_쿼리_수는_같다() {
		openRooms(2);
		long few = countQueries();

		openRooms(4);
		long many = countQueries();

		assertThat(chatRoomService.getList(me, PageRequest.of(0, 20)).items()).hasSize(6)
				.allSatisfy(room -> assertThat(room.lastMessage()).isNotNull());
		assertThat(many).isEqualTo(few);
	}

	@Test
	void 목록의_마지막_메시지와_안_읽은_수는_방마다_따로_센다() {
		Long kim = alumni("김사자");
		Long lee = alumni("이사자");
		Long withKim = room(kim);
		Long withLee = room(lee);
		send(kim, withKim, "하나");
		send(kim, withKim, "둘");
		send(me, withLee, "셋");
		send(lee, withLee, "넷");

		List<ChatRoomSummaryResponse> rooms = chatRoomService.getList(me, PageRequest.of(0, 20)).items();
		assertThat(rooms).extracting(ChatRoomSummaryResponse::roomId, room -> room.lastMessage().content(), ChatRoomSummaryResponse::unreadCount)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple(withLee, "넷", 1L),
						org.assertj.core.groups.Tuple.tuple(withKim, "둘", 2L));
	}

	private long countQueries() {
		entityManager.flush();
		entityManager.clear();
		Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		statistics.setStatisticsEnabled(true);
		statistics.clear();
		try {
			chatRoomService.getList(me, PageRequest.of(0, 20));
			return statistics.getPrepareStatementCount();
		} finally {
			statistics.setStatisticsEnabled(false);
		}
	}

	private void openRooms(int count) {
		for (int i = 0; i < count; i++) {
			Long other = alumni("동문" + i);
			Long roomId = room(other);
			send(other, roomId, "안녕하세요 " + i);
		}
	}

	private Long room(Long other) {
		Long roomId = chatRequestService.create(other, new ChatRequestCreateRequest(me, "안녕하세요! 이야기 나눠요.")).roomId();
		chatRequestService.accept(me, roomId);
		return roomId;
	}

	private void send(Long userId, Long roomId, String content) {
		chatRoomService.send(userId, roomId, new ChatMessageSendRequest(content, null));
	}

	private ChatRoomDetailResponse detail(Long roomId) {
		entityManager.flush();
		entityManager.clear();
		return chatRoomService.getDetail(me, roomId);
	}

	private Long alumni(String name) {
		Long userId = TestUsers.approved(entityManager, name);
		TestProfiles.save(entityManager, TestProfiles.profile(userId));
		return userId;
	}
}
