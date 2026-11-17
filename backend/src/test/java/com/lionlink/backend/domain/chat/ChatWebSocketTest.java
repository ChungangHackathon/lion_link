package com.lionlink.backend.domain.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionlink.backend.domain.chat.dto.request.ChatMessageSendRequest;
import com.lionlink.backend.domain.chat.dto.request.ChatRequestCreateRequest;
import com.lionlink.backend.domain.chat.service.ChatRequestService;
import com.lionlink.backend.domain.chat.service.ChatRoomService;
import com.lionlink.backend.global.security.DevHeaderCurrentUserProvider;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatWebSocketTest {

	private static final long TIMEOUT = 5;

	@LocalServerPort
	private int port;

	@Autowired
	private ChatRequestService chatRequestService;

	@Autowired
	private ChatRoomService chatRoomService;

	@Autowired
	private SimpUserRegistry userRegistry;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private final List<Long> userIds = new ArrayList<>();
	private final List<StompSession> sessions = new ArrayList<>();

	@AfterEach
	void tearDown() {
		sessions.stream().filter(StompSession::isConnected).forEach(StompSession::disconnect);
		userIds.forEach(userId -> {
			jdbcTemplate.update("delete from notifications where user_id = ?", userId);
			jdbcTemplate.update("delete from email_logs where user_id = ?", userId);
			jdbcTemplate.update("delete from chat_messages where room_id in (select room_id from chat_room_members where user_id = ?)", userId);
			jdbcTemplate.update("delete from chat_room_members where room_id in (select id from chat_rooms where user_a_id = ? or user_b_id = ?)", userId, userId);
			jdbcTemplate.update("delete from chat_rooms where user_a_id = ? or user_b_id = ?", userId, userId);
		});
		userIds.forEach(userId -> jdbcTemplate.update("delete from users where id = ?", userId));
	}

	@Test
	void 요청과_메시지가_구독한_사람에게_실시간으로_간다() throws Exception {
		Long me = user("나사자");
		Long kim = user("김사자");

		BlockingQueue<Map<String, Object>> kimQueue = subscribe(connect(kim), "/user/queue/chat");
		Long roomId = chatRequestService.create(me, new ChatRequestCreateRequest(kim, "안녕하세요! 이야기 나눠요.")).roomId();

		Map<String, Object> received = kimQueue.poll(TIMEOUT, TimeUnit.SECONDS);
		assertThat(received).containsEntry("type", "REQUEST_RECEIVED");
		assertThat(((Number) received.get("roomId")).longValue()).isEqualTo(roomId);

		chatRequestService.accept(kim, roomId);
		BlockingQueue<Map<String, Object>> roomQueue = subscribe(connect(me), "/topic/chat-rooms/" + roomId);
		chatRoomService.send(kim, roomId, new ChatMessageSendRequest("반가워요", "c-1"));

		Map<String, Object> event = roomQueue.poll(TIMEOUT, TimeUnit.SECONDS);
		assertThat(event).containsEntry("type", "MESSAGE");
		@SuppressWarnings("unchecked")
		Map<String, Object> message = (Map<String, Object>) event.get("message");
		// clientMsgId가 실려야 보낸 사람 화면이 미리 그려 둔 메시지와 짝지어 지울 수 있다
		assertThat(message).containsEntry("content", "반가워요").containsEntry("unreadCount", 1).containsEntry("clientMsgId", "c-1");
		assertThat((String) message.get("createdAt")).endsWith("+09:00");
	}

	@Test
	void 참여하지_않은_방은_구독할_수_없고_로그인_없이는_연결할_수_없다() throws Exception {
		Long me = user("나사자");
		Long kim = user("김사자");
		Long stranger = user("모르는사자");
		Long roomId = chatRequestService.create(me, new ChatRequestCreateRequest(kim, "안녕하세요! 이야기 나눠요.")).roomId();

		CompletableFuture<String> error = new CompletableFuture<>();
		StompSession session = connect(stranger, error);
		session.subscribe("/topic/chat-rooms/" + roomId, frameHandler(new LinkedBlockingQueue<>()));

		assertThat(error.get(TIMEOUT, TimeUnit.SECONDS)).isEqualTo("참여 중인 채팅방만 구독할 수 있어요.");
		assertThatThrownBy(() -> client().connectAsync(url(), new WebSocketHttpHeaders(), new StompHeaders(),
				new StompSessionHandlerAdapter() {
				}).get(TIMEOUT, TimeUnit.SECONDS)).isNotNull();
	}

	private Long user(String name) {
		Long userId = transactionTemplate.execute(status -> TestUsers.approved(entityManager, name));
		userIds.add(userId);
		return userId;
	}

	private StompSession connect(Long userId) throws Exception {
		return connect(userId, new CompletableFuture<>());
	}

	private StompSession connect(Long userId, CompletableFuture<String> error) throws Exception {
		StompHeaders headers = new StompHeaders();
		headers.add(DevHeaderCurrentUserProvider.HEADER, String.valueOf(userId));
		StompSession session = client().connectAsync(url(), new WebSocketHttpHeaders(), headers, new StompSessionHandlerAdapter() {

			@Override
			public void handleFrame(StompHeaders headers, Object payload) {
				error.complete(headers.getFirst("message"));
			}
		}).get(TIMEOUT, TimeUnit.SECONDS);
		sessions.add(session);
		return session;
	}

	private BlockingQueue<Map<String, Object>> subscribe(StompSession session, String destination) throws InterruptedException {
		BlockingQueue<Map<String, Object>> queue = new LinkedBlockingQueue<>();
		session.subscribe(destination, frameHandler(queue));
		long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT);
		while (userRegistry.findSubscriptions(subscription -> subscription.getDestination().equals(destination)).isEmpty()) {
			assertThat(System.currentTimeMillis()).isLessThan(deadline);
			Thread.sleep(20);
		}
		return queue;
	}

	private StompFrameHandler frameHandler(BlockingQueue<Map<String, Object>> queue) {
		return new StompFrameHandler() {

			@Override
			public Type getPayloadType(StompHeaders headers) {
				return Map.class;
			}

			@Override
			@SuppressWarnings("unchecked")
			public void handleFrame(StompHeaders headers, Object payload) {
				queue.add((Map<String, Object>) payload);
			}
		};
	}

	private WebSocketStompClient client() {
		WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
		client.setMessageConverter(new JacksonJsonMessageConverter(jsonMapper));
		return client;
	}

	private String url() {
		return "ws://localhost:" + port + "/ws";
	}
}
