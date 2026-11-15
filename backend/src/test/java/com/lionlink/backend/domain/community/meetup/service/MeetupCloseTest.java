package com.lionlink.backend.domain.community.meetup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionlink.backend.domain.chat.dto.request.ChatRoomInviteRequest;
import com.lionlink.backend.domain.chat.entity.ChatRoom;
import com.lionlink.backend.domain.chat.exception.ChatErrorCode;
import com.lionlink.backend.domain.chat.repository.ChatRoomRepository;
import com.lionlink.backend.domain.chat.service.ChatRoomService;
import com.lionlink.backend.domain.community.exception.CommunityErrorCode;
import com.lionlink.backend.domain.community.meetup.entity.MeetupStatus;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MeetupCloseTest {

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
	void 정원이_남아도_작성자가_마감할_수_있다() {
		Long postId = postService.create(me, TestPosts.meetup("금요일 저녁 번개", 10, List.of(kim))).id();

		assertThat(meetupService.close(me, postId).status()).isEqualTo(MeetupStatus.FULL);

		assertError(() -> meetupService.join(lee, postId), CommunityErrorCode.MEETUP_FULL);
	}

	@Test
	void 마감을_해제하면_다시_참여할_수_있다() {
		Long postId = postService.create(me, TestPosts.meetup("금요일 저녁 번개", 10, List.of(kim))).id();
		meetupService.close(me, postId);

		assertThat(meetupService.reopen(me, postId).status()).isEqualTo(MeetupStatus.OPEN);

		assertThat(meetupService.join(lee, postId).joined()).isTrue();
	}

	@Test
	void 이미_참여한_사람은_마감_뒤에도_그대로다() {
		Long postId = postService.create(me, TestPosts.meetup("금요일 저녁 번개", 10, List.of(kim))).id();
		meetupService.close(me, postId);

		assertThat(meetupService.join(kim, postId).joined()).isTrue();
	}

	@Test
	void 작성자가_아니면_마감할_수_없다() {
		Long postId = postService.create(me, TestPosts.meetup("금요일 저녁 번개", 10, List.of(kim))).id();

		assertError(() -> meetupService.close(kim, postId), CommunityErrorCode.NOT_OWNER);
		assertError(() -> meetupService.reopen(kim, postId), CommunityErrorCode.NOT_OWNER);
	}

	@Test
	void 취소된_번개는_마감할_수_없다() {
		Long postId = postService.create(me, TestPosts.meetup("금요일 저녁 번개", 10, List.of(kim))).id();
		postService.delete(me, postId);
		flush();

		assertError(() -> meetupService.close(me, postId), CommunityErrorCode.POST_NOT_FOUND);
	}

	@Test
	void 마감해도_채팅방은_열려_있다() {
		Long postId = postService.create(me, TestPosts.meetup("금요일 저녁 번개", 10, List.of(kim))).id();
		meetupService.close(me, postId);
		flush();
		Long roomId = chatRoomRepository.findByMeetupId(postId).map(ChatRoom::getId).orElseThrow();

		assertThat(chatRoomService.getDetail(kim, roomId).canSend()).isTrue();
	}

	@Test
	void 마감된_번개방에는_초대할_수_없다() {
		Long postId = postService.create(me, TestPosts.meetup("금요일 저녁 번개", 10, List.of(kim))).id();
		meetupService.close(me, postId);
		flush();
		Long roomId = chatRoomRepository.findByMeetupId(postId).map(ChatRoom::getId).orElseThrow();

		assertError(() -> chatRoomService.invite(me, roomId, new ChatRoomInviteRequest(List.of(lee))),
				ChatErrorCode.ROOM_FULL);
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
