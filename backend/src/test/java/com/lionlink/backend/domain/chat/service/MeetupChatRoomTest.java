package com.lionlink.backend.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionlink.backend.domain.chat.dto.request.ChatMessageSendRequest;
import com.lionlink.backend.domain.chat.dto.response.ChatMessageResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatRoomDetailResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.lionlink.backend.domain.chat.dto.response.ChatUserResponse;
import com.lionlink.backend.domain.chat.entity.ChatRoom;
import com.lionlink.backend.domain.chat.entity.ChatRoomType;
import com.lionlink.backend.domain.chat.exception.ChatErrorCode;
import com.lionlink.backend.domain.chat.repository.ChatRoomRepository;
import com.lionlink.backend.domain.community.meetup.entity.MeetupStatus;
import com.lionlink.backend.domain.community.meetup.service.MeetupService;
import com.lionlink.backend.domain.community.post.dto.request.PostUpdateRequest;
import com.lionlink.backend.domain.community.post.entity.PostCategory;
import com.lionlink.backend.domain.community.post.service.PostService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MeetupChatRoomTest {

	@Autowired
	private PostService postService;

	@Autowired
	private MeetupService meetupService;

	@Autowired
	private ChatRoomService chatRoomService;

	@Autowired
	private ChatRoomRepository chatRoomRepository;

	@Autowired
	private EntityManager entityManager;

	private Long me;
	private Long kim;
	private Long lee;

	@BeforeEach
	void setUp() {
		me = alumni("나사자");
		kim = alumni("김사자");
		lee = alumni("이사자");
	}

	@Test
	void 번개_글을_쓰면_작성자와_함께하는_동문이_있는_단체방이_열린다() {
		Long postId = postService.create(me, TestPosts.meetup("금요일 저녁 번개", 4, List.of(kim))).id();
		Long roomId = roomId(postId);

		ChatRoomSummaryResponse summary = chatRoomService.getList(me, PageRequest.of(0, 20)).items().get(0);
		assertThat(summary.roomId()).isEqualTo(roomId);
		assertThat(summary.type()).isEqualTo(ChatRoomType.GROUP);
		assertThat(summary.meetupId()).isEqualTo(postId);
		assertThat(summary.title()).isEqualTo("금요일 저녁 번개");
		assertThat(summary.members()).extracting(ChatUserResponse::id).containsExactly(kim);
		assertThat(summary.lastMessage().content()).isEqualTo("번개 채팅방이 열렸어요. 모임 전에 인사 나눠보세요.");

		ChatRoomDetailResponse detail = chatRoomService.getDetail(kim, roomId);
		assertThat(detail.meetup().postId()).isEqualTo(postId);
		assertThat(detail.meetup().title()).isEqualTo("금요일 저녁 번개");
		assertThat(detail.meetup().place()).isEqualTo("흑석역 2번 출구");
		assertThat(detail.meetup().status()).isEqualTo(MeetupStatus.OPEN);
		assertThat(detail.coffeeChat()).isNull();
		assertThat(detail.canSend()).isTrue();
	}

	@Test
	void 번개에_참여하면_방에_들어오고_참여를_취소하면_나간다() {
		Long postId = postService.create(me, TestPosts.meetup("금요일 저녁 번개", 4, null)).id();
		Long roomId = roomId(postId);

		meetupService.join(lee, postId);
		meetupService.join(lee, postId);
		assertThat(lastMessage(me, roomId)).isEqualTo("이사자님이 들어왔어요.");
		assertThat(chatRoomService.getDetail(lee, roomId).members()).extracting(ChatUserResponse::id).containsExactly(me);
		chatRoomService.send(lee, roomId, new ChatMessageSendRequest("반가워요", null));

		meetupService.leave(lee, postId);
		assertThat(lastMessage(me, roomId)).isEqualTo("이사자님이 나갔어요.");
		assertError(() -> chatRoomService.getDetail(lee, roomId), ChatErrorCode.ROOM_NOT_FOUND);

		meetupService.join(lee, postId);
		assertThat(chatRoomService.getDetail(lee, roomId).canSend()).isTrue();
		assertThat(chatRoomService.getMessages(me, roomId, null, null, 30).items()).extracting(ChatMessageResponse::content)
				.containsExactly("번개 채팅방이 열렸어요. 모임 전에 인사 나눠보세요.", "이사자님이 들어왔어요.", "반가워요",
						"이사자님이 나갔어요.", "이사자님이 들어왔어요.");
	}

	@Test
	void 번개가_끝나기_전에는_채팅방만_나갈_수_없다() {
		Long postId = postService.create(me, TestPosts.meetup("금요일 저녁 번개", 4, List.of(kim))).id();
		Long roomId = roomId(postId);

		assertError(() -> chatRoomService.leave(kim, roomId), ChatErrorCode.INVALID_ROOM_STATUS);
		assertError(() -> chatRoomService.leave(me, roomId), ChatErrorCode.INVALID_ROOM_STATUS);

		TestPosts.update(entityManager, "update meetups set meet_at = dateadd('HOUR', -1, current_timestamp) where post_id = :id", postId);
		chatRoomService.leave(kim, roomId);
		assertThat(lastMessage(me, roomId)).isEqualTo("김사자님이 나갔어요.");
		assertThat(chatRoomService.getDetail(me, roomId).meetup().status()).isEqualTo(MeetupStatus.ENDED);
	}

	@Test
	void 번개를_취소하면_안내가_남고_더_이상_보낼_수_없다() {
		Long postId = postService.create(me, TestPosts.meetup("금요일 저녁 번개", 4, List.of(kim))).id();
		Long roomId = roomId(postId);

		postService.delete(me, postId);
		flush();

		assertThat(lastMessage(kim, roomId)).isEqualTo(MeetupChatRoomService.CANCELED);
		ChatRoomDetailResponse detail = chatRoomService.getDetail(kim, roomId);
		assertThat(detail.canSend()).isFalse();
		assertThat(detail.meetup().status()).isEqualTo(MeetupStatus.CANCELED);
		assertError(() -> chatRoomService.send(kim, roomId, new ChatMessageSendRequest("아쉬워요", null)), ChatErrorCode.INVALID_ROOM_STATUS);
		assertThat(chatRoomService.getList(kim, PageRequest.of(0, 20)).items()).extracting(ChatRoomSummaryResponse::roomId)
				.containsExactly(roomId);

		chatRoomService.leave(kim, roomId);
		assertThat(chatRoomService.getList(kim, PageRequest.of(0, 20)).items()).isEmpty();
	}

	@Test
	void 번개가_아닌_글로_바꾸면_방도_지운다() {
		Long postId = postService.create(me, TestPosts.meetup("금요일 저녁 번개", 4, null)).id();
		roomId(postId);

		postService.update(me, postId, new PostUpdateRequest(PostCategory.FREE, null, null, null, null, null, null, null));
		flush();

		assertThat(chatRoomRepository.findByMeetupId(postId)).isEmpty();
		assertThat(chatRoomService.getList(me, PageRequest.of(0, 20)).items()).isEmpty();
	}

	private Long roomId(Long postId) {
		flush();
		return chatRoomRepository.findByMeetupId(postId).map(ChatRoom::getId).orElseThrow();
	}

	private String lastMessage(Long userId, Long roomId) {
		flush();
		List<ChatMessageResponse> items = chatRoomService.getMessages(userId, roomId, null, null, 1).items();
		return items.get(items.size() - 1).content();
	}

	private Long alumni(String name) {
		Long userId = TestUsers.approved(entityManager, name);
		TestProfiles.save(entityManager, TestProfiles.profile(userId));
		return userId;
	}

	private void flush() {
		entityManager.flush();
		entityManager.clear();
	}

	private void assertError(ThrowingCallable callable, ErrorCode errorCode) {
		assertThatThrownBy(callable)
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(errorCode);
	}
}
