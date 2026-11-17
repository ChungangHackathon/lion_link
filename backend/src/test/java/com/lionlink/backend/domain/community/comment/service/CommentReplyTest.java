package com.lionlink.backend.domain.community.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionlink.backend.domain.community.comment.dto.request.CommentCreateRequest;
import com.lionlink.backend.domain.community.comment.dto.response.CommentResponse;
import com.lionlink.backend.domain.community.exception.CommunityErrorCode;
import com.lionlink.backend.domain.community.post.entity.PostCategory;
import com.lionlink.backend.domain.community.post.service.PostService;
import com.lionlink.backend.domain.notification.entity.Notification;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CommentReplyTest {

	@Autowired
	private CommentService commentService;

	@Autowired
	private PostService postService;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private EntityManager entityManager;

	private Long me;
	private Long kim;
	private Long lee;
	private Long postId;

	@BeforeEach
	void setUp() {
		me = alumni("나사자");
		kim = alumni("김사자");
		lee = alumni("이사자");
		postId = postService.create(me, TestPosts.general(PostCategory.FREE, "질문이 있어요")).id();
	}

	@Test
	void 답글을_달면_부모_댓글_아이디가_담긴다() {
		Long parentId = comment(kim, "저도 궁금해요", null).id();

		CommentResponse reply = comment(lee, "이렇게 해보세요", parentId);

		assertThat(reply.parentId()).isEqualTo(parentId);
		assertThat(comment(kim, "새 댓글", null).parentId()).isNull();
	}

	@Test
	void 목록은_부모_바로_뒤에_답글이_온다() {
		Long first = comment(kim, "첫 댓글", null).id();
		Long second = comment(lee, "둘째 댓글", null).id();
		Long reply = comment(me, "첫 댓글의 답글", first).id();
		flush();

		List<CommentResponse> comments = commentService.getComments(me, postId, PageRequest.of(0, 20)).items();

		assertThat(comments).extracting(CommentResponse::id).containsExactly(first, reply, second);
	}

	@Test
	void 답글에는_답글을_달_수_없다() {
		Long parentId = comment(kim, "부모 댓글", null).id();
		Long replyId = comment(lee, "답글", parentId).id();

		assertError(() -> comment(me, "답글의 답글", replyId), CommunityErrorCode.PARENT_COMMENT_NOT_FOUND);
	}

	@Test
	void 다른_글의_댓글에는_답글을_달_수_없다() {
		Long otherPostId = postService.create(me, TestPosts.general(PostCategory.FREE, "다른 글")).id();
		Long parentId = comment(kim, "다른 글의 댓글", null).id();

		assertError(() -> commentService.create(me, otherPostId, new CommentCreateRequest("답글", parentId)),
				CommunityErrorCode.PARENT_COMMENT_NOT_FOUND);
	}

	@Test
	void 지운_댓글에는_답글을_달_수_없다() {
		Long parentId = comment(kim, "지울 댓글", null).id();
		commentService.delete(kim, postId, parentId);

		assertError(() -> comment(lee, "답글", parentId), CommunityErrorCode.PARENT_COMMENT_NOT_FOUND);
	}

	@Test
	void 부모를_지워도_답글은_남는다() {
		Long parentId = comment(kim, "지울 댓글", null).id();
		Long replyId = comment(lee, "남을 답글", parentId).id();
		commentService.delete(kim, postId, parentId);
		flush();

		List<CommentResponse> comments = commentService.getComments(me, postId, PageRequest.of(0, 20)).items();

		assertThat(comments).extracting(CommentResponse::id).containsExactly(parentId, replyId);
		assertThat(comments.get(0).deleted()).isTrue();
		assertThat(comments.get(1).content()).isEqualTo("남을 답글");
	}

	@Test
	void 답글을_달면_부모_댓글_작성자에게_알림이_간다() {
		Long parentId = comment(kim, "부모 댓글", null).id();

		comment(lee, "답글", parentId);
		flush();

		assertThat(replyNotifications()).singleElement()
				.satisfies(notification -> {
					assertThat(notification.getUserId()).isEqualTo(kim);
					assertThat(notification.getActorId()).isEqualTo(lee);
					assertThat(notification.getMessage()).isEqualTo("님이 내 댓글에 답글을 남겼어요. 질문이 있어요");
				});
	}

	@Test
	void 내_댓글에_내가_답글을_달면_알림이_없다() {
		Long parentId = comment(kim, "부모 댓글", null).id();

		comment(kim, "자답", parentId);
		flush();

		assertThat(replyNotifications()).isEmpty();
	}

	@Test
	void 글쓴이가_쓴_댓글에_답글을_달면_알림은_한_번만_간다() {
		Long parentId = comment(me, "글쓴이 댓글", null).id();

		comment(kim, "답글", parentId);
		flush();

		assertThat(replyNotifications()).isEmpty();
	}

	private CommentResponse comment(Long userId, String content, Long parentId) {
		return commentService.create(userId, postId, new CommentCreateRequest(content, parentId));
	}

	private List<Notification> replyNotifications() {
		return notificationRepository.findAll().stream()
				.filter(notification -> "REPLY_CREATED".equals(notification.getType()))
				.toList();
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
