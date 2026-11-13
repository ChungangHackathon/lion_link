package com.lionlink.backend.domain.community.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.lionlink.backend.domain.community.comment.dto.request.CommentCreateRequest;
import com.lionlink.backend.domain.community.comment.dto.response.CommentResponse;
import com.lionlink.backend.domain.community.exception.CommunityErrorCode;
import com.lionlink.backend.domain.community.post.dto.request.PostCreateRequest;
import com.lionlink.backend.domain.community.post.entity.PostCategory;
import com.lionlink.backend.domain.community.post.service.PostQueryService;
import com.lionlink.backend.domain.community.post.service.PostService;
import com.lionlink.backend.domain.notification.entity.Notification;
import com.lionlink.backend.domain.notification.entity.NotificationTab;
import com.lionlink.backend.domain.notification.entity.NotificationTargetType;
import com.lionlink.backend.domain.notification.repository.NotificationRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.ErrorCode;
import com.lionlink.backend.global.response.SliceResponse;
import com.lionlink.backend.support.TestPosts;
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
class CommentServiceTest {

	@Autowired
	private CommentService commentService;

	@Autowired
	private PostService postService;

	@Autowired
	private PostQueryService postQueryService;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private EntityManager entityManager;

	private Long me;
	private Long kim;
	private Long postId;

	@BeforeEach
	void setUp() {
		me = TestUsers.approved(entityManager, "나사자");
		kim = TestUsers.approved(entityManager, "김사자");
		postId = postService.create(me, TestPosts.general(PostCategory.FREE, "자유 글")).id();
	}

	@Test
	void 댓글을_작성하면_작성자_정보와_함께_돌려준다() {
		CommentResponse response = commentService.create(kim, postId, request("  좋은 글이에요  "));

		assertThat(response.content()).isEqualTo("좋은 글이에요");
		assertThat(response.postId()).isEqualTo(postId);
		assertThat(response.isAnonymous()).isFalse();
		assertThat(response.author().name()).isEqualTo("김사자");
		assertThat(response.isMine()).isTrue();
		assertThat(response.deleted()).isFalse();
	}

	@Test
	void 댓글을_작성하면_글_작성자에게_알림이_간다() {
		commentService.create(kim, postId, request("좋은 글이에요"));

		List<Notification> notifications = commentNotifications();
		assertThat(notifications).hasSize(1);
		Notification notification = notifications.get(0);
		assertThat(notification.getUserId()).isEqualTo(me);
		assertThat(notification.getTab()).isEqualTo(NotificationTab.COMMUNITY);
		assertThat(notification.getTargetType()).isEqualTo(NotificationTargetType.POST);
		assertThat(notification.getTargetId()).isEqualTo(postId);
		assertThat(notification.getMessage()).isEqualTo("김사자님이 내 글에 댓글을 남겼어요. 자유 글");
	}

	@Test
	void 내_글에_내가_쓴_댓글은_알림을_보내지_않는다() {
		commentService.create(me, postId, request("보충 설명이에요"));
		Long anonymousPostId = postService.create(me, new PostCreateRequest(PostCategory.FREE, "익명 고민", "고민이 있어요.",
				null, true, null, null, null, null)).id();
		commentService.create(me, anonymousPostId, request("작성자 답변이에요"));

		assertThat(commentNotifications()).isEmpty();
	}

	@Test
	void 익명_글에_달린_댓글도_글_작성자에게_알림이_간다() {
		Long anonymousPostId = postService.create(me, new PostCreateRequest(PostCategory.FREE, "익명 고민", "고민이 있어요.",
				null, true, null, null, null, null)).id();

		commentService.create(kim, anonymousPostId, request("힘내세요"));

		assertThat(commentNotifications()).extracting(Notification::getUserId, Notification::getMessage)
				.containsExactly(tuple(me, "김사자님이 내 글에 댓글을 남겼어요. 익명 고민"));
	}

	@Test
	void 익명_글의_작성자가_쓴_댓글만_익명이_된다() {
		Long anonymousPostId = postService.create(me, new PostCreateRequest(PostCategory.FREE, "익명 고민", "고민이 있어요.",
				null, true, null, null, null, null)).id();

		CommentResponse byAuthor = commentService.create(me, anonymousPostId, request("작성자 댓글"));
		CommentResponse byOther = commentService.create(kim, anonymousPostId, request("다른 사람 댓글"));
		CommentResponse onNamedPost = commentService.create(me, postId, request("실명 글 댓글"));

		assertThat(byAuthor.isAnonymous()).isTrue();
		assertThat(byAuthor.author()).isNull();
		assertThat(byOther.isAnonymous()).isFalse();
		assertThat(byOther.author().name()).isEqualTo("김사자");
		assertThat(onNamedPost.isAnonymous()).isFalse();
	}

	@Test
	void 목록은_오래된_순이고_삭제된_댓글은_내용과_작성자를_숨긴다() {
		Long first = commentService.create(kim, postId, request("첫 댓글")).id();
		Long second = commentService.create(me, postId, request("둘째 댓글")).id();
		commentService.delete(kim, postId, first);
		entityManager.flush();
		entityManager.clear();

		SliceResponse<CommentResponse> result = commentService.getComments(me, postId, PageRequest.of(0, 20));

		assertThat(result.items()).extracting(CommentResponse::id).containsExactly(first, second);
		CommentResponse deleted = result.items().get(0);
		assertThat(deleted.deleted()).isTrue();
		assertThat(deleted.content()).isEqualTo("삭제된 댓글이에요.");
		assertThat(deleted.author()).isNull();
		assertThat(deleted.isMine()).isFalse();
		assertThat(result.items().get(1).isMine()).isTrue();
		assertThat(result.hasNext()).isFalse();
	}

	@Test
	void 탈퇴한_작성자의_댓글은_탈퇴_표시를_한다() {
		commentService.create(kim, postId, request("탈퇴 전 댓글"));
		TestUsers.withdraw(entityManager, kim);

		CommentResponse comment = commentService.getComments(me, postId, PageRequest.of(0, 20)).items().get(0);

		assertThat(comment.author().isDeleted()).isTrue();
		assertThat(comment.author().name()).isNull();
	}

	@Test
	void 삭제된_댓글은_게시글_댓글_수에서_빠진다() {
		Long commentId = commentService.create(kim, postId, request("댓글")).id();
		commentService.create(me, postId, request("댓글 2"));
		commentService.delete(kim, postId, commentId);
		entityManager.flush();
		entityManager.clear();

		assertThat(postQueryService.getDetail(me, postId).commentCount()).isEqualTo(1);
	}

	@Test
	void 작성자나_관리자만_삭제할_수_있다() {
		Long commentId = commentService.create(kim, postId, request("김사자 댓글")).id();
		Long admin = TestUsers.approved(entityManager, "관리자");
		entityManager.createNativeQuery("update users set role = 'ADMIN' where id = :id").setParameter("id", admin).executeUpdate();

		assertError(() -> commentService.delete(me, postId, commentId), CommunityErrorCode.NOT_OWNER);

		commentService.delete(admin, postId, commentId);
		assertError(() -> commentService.delete(kim, postId, commentId), CommunityErrorCode.COMMENT_NOT_FOUND);
	}

	@Test
	void 다른_게시글의_댓글은_찾을_수_없다() {
		Long otherPostId = postService.create(me, TestPosts.general(PostCategory.FREE, "다른 글")).id();
		Long commentId = commentService.create(kim, postId, request("댓글")).id();

		assertError(() -> commentService.delete(kim, otherPostId, commentId), CommunityErrorCode.COMMENT_NOT_FOUND);
	}

	@Test
	void 삭제된_게시글에는_댓글을_달거나_조회할_수_없다() {
		postService.delete(me, postId);

		assertError(() -> commentService.create(kim, postId, request("댓글")), CommunityErrorCode.POST_NOT_FOUND);
		assertError(() -> commentService.getComments(kim, postId, PageRequest.of(0, 20)), CommunityErrorCode.POST_NOT_FOUND);
	}

	private CommentCreateRequest request(String content) {
		return new CommentCreateRequest(content);
	}

	private void assertError(ThrowingCallable callable, ErrorCode errorCode) {
		assertThatThrownBy(callable)
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(errorCode);
	}

	private List<Notification> commentNotifications() {
		return notificationRepository.findAll().stream()
				.filter(notification -> notification.getType().equals("COMMENT_CREATED"))
				.toList();
	}
}
