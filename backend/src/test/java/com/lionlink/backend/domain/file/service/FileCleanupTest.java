package com.lionlink.backend.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.lionlink.backend.domain.community.post.dto.request.PostCreateRequest;
import com.lionlink.backend.domain.community.post.dto.request.PostFileRequest;
import com.lionlink.backend.domain.community.post.dto.request.PostUpdateRequest;
import com.lionlink.backend.domain.community.post.entity.PostCategory;
import com.lionlink.backend.domain.community.post.entity.PostFileType;
import com.lionlink.backend.domain.community.post.service.PostService;
import com.lionlink.backend.support.TestProfiles;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@RecordApplicationEvents
class FileCleanupTest {

	private static final String BASE_URL = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/";

	@MockitoBean
	private FileStorage fileStorage;

	@Autowired
	private PostService postService;

	@Autowired
	private FileCleanupService fileCleanupService;

	@Autowired
	private ApplicationEvents events;

	@Autowired
	private EntityManager entityManager;

	private Long me;

	@BeforeEach
	void setUp() {
		me = TestUsers.approved(entityManager, "나사자");
		TestProfiles.save(entityManager, TestProfiles.profile(me));
	}

	@Test
	void 글을_고치면_빠진_파일만_지우고_저장이_끝나기_전에는_지우지_않는다() {
		PostFileRequest first = image();
		PostFileRequest second = image();
		Long postId = create(List.of(first, second));

		postService.update(me, postId, new PostUpdateRequest(null, null, null, null, null, null, null, List.of(second, image())));

		assertThat(requestedUrls()).containsExactly(first.url());
		verify(fileStorage, never()).delete(any());
	}

	@Test
	void 글을_지우면_다른_글이나_프로필_사진에서_쓰지_않는_파일만_지운다() {
		PostFileRequest only = image();
		PostFileRequest shared = image();
		PostFileRequest photo = image();
		Long postId = create(List.of(only, shared, photo));
		create(List.of(shared));
		entityManager.createNativeQuery("update profiles set photo_url = :url where user_id = :userId")
				.setParameter("url", photo.url())
				.setParameter("userId", me)
				.executeUpdate();

		postService.delete(me, postId);

		assertThat(requestedUrls()).containsExactly(only.url());
	}

	@Test
	void 업로드_경로의_파일만_지우고_실패해도_예외를_내지_않는다() {
		String key = "uploads/" + me + "/" + UUID.randomUUID() + ".png";

		fileCleanupService.onDeleteRequested(new FileDeleteRequestedEvent(List.of(
				BASE_URL + key, BASE_URL + key, "https://example.com/uploads/a.png", BASE_URL + "other/a.png")));
		verify(fileStorage).delete(key);
		verifyNoMoreInteractions(fileStorage);

		doThrow(new IllegalStateException("S3 down")).when(fileStorage).delete(any());
		assertThatCode(() -> fileCleanupService.onDeleteRequested(new FileDeleteRequestedEvent(List.of(BASE_URL + key))))
				.doesNotThrowAnyException();
	}

	private List<String> requestedUrls() {
		return events.stream(FileDeleteRequestedEvent.class).flatMap(event -> event.urls().stream()).toList();
	}

	private Long create(List<PostFileRequest> files) {
		return postService.create(me, new PostCreateRequest(PostCategory.MATERIAL, "족보 공유", "자료 올려요.",
				null, false, null, null, null, files)).id();
	}

	private PostFileRequest image() {
		return new PostFileRequest(PostFileType.IMAGE, BASE_URL + "uploads/" + me + "/" + UUID.randomUUID() + ".png", "a.png", 1024);
	}
}
