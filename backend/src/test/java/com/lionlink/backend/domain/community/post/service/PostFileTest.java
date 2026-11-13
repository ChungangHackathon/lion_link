package com.lionlink.backend.domain.community.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionlink.backend.domain.community.exception.CommunityErrorCode;
import com.lionlink.backend.domain.community.post.dto.request.PostCreateRequest;
import com.lionlink.backend.domain.community.post.dto.request.PostFileRequest;
import com.lionlink.backend.domain.community.post.dto.request.PostSearchCondition;
import com.lionlink.backend.domain.community.post.dto.request.PostSearchCondition.PostSort;
import com.lionlink.backend.domain.community.post.dto.request.PostUpdateRequest;
import com.lionlink.backend.domain.community.post.dto.response.PostDetailResponse;
import com.lionlink.backend.domain.community.post.dto.response.PostSummaryResponse;
import com.lionlink.backend.domain.community.post.entity.PostCategory;
import com.lionlink.backend.domain.community.post.entity.PostFileType;
import com.lionlink.backend.domain.file.exception.FileErrorCode;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.ErrorCode;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PostFileTest {

	private static final String BASE_URL = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/uploads/";

	@Autowired
	private PostService postService;

	@Autowired
	private PostQueryService postQueryService;

	@Autowired
	private EntityManager entityManager;

	private Long me;

	@BeforeEach
	void setUp() {
		me = TestUsers.approved(entityManager, "나사자");
	}

	@Test
	void 글을_쓸_때_올린_파일을_순서대로_저장한다() {
		PostFileRequest photo = image("photo.png");
		PostFileRequest guide = attachment("안내문.pdf");
		Long postId = create(List.of(guide, photo));

		PostDetailResponse detail = postQueryService.getDetail(me, postId);
		assertThat(detail.files()).extracting(PostDetailResponse.FileResponse::url).containsExactly(guide.url(), photo.url());
		assertThat(detail.files().get(0).type()).isEqualTo(PostFileType.ATTACHMENT);
		assertThat(detail.files().get(0).name()).isEqualTo("안내문.pdf");
		assertThat(detail.files().get(0).size()).isEqualTo(2048);

		PostSummaryResponse item = postQueryService.search(
				new PostSearchCondition(null, null, PostSort.LATEST, false, null), PageRequest.of(0, 20)).items().get(0);
		assertThat(item.hasFile()).isTrue();
		assertThat(item.thumbnailUrl()).isEqualTo(photo.url());
	}

	@Test
	void 수정할_때_파일을_보내면_통째로_바꾸고_안_보내면_그대로_둔다() {
		PostFileRequest first = image("first.png");
		Long postId = create(List.of(first, attachment("old.pdf")));

		postService.update(me, postId, new PostUpdateRequest(null, "제목 수정", null, null, null, null, null, null));
		flush();
		assertThat(fileUrls(postId)).hasSize(2);

		PostFileRequest second = image("second.webp");
		postService.update(me, postId, new PostUpdateRequest(null, null, null, null, null, null, null, List.of(second, first)));
		flush();
		assertThat(fileUrls(postId)).containsExactly(second.url(), first.url());

		postService.update(me, postId, new PostUpdateRequest(null, null, null, null, null, null, null, List.of()));
		flush();
		assertThat(fileUrls(postId)).isEmpty();
	}

	@Test
	void 이미지는_5장_첨부파일은_3개까지_붙일_수_있다() {
		List<PostFileRequest> fiveImages = IntStream.range(0, 5).mapToObj(i -> image(i + ".png")).toList();
		List<PostFileRequest> threeAttachments = IntStream.range(0, 3).mapToObj(i -> attachment(i + ".pdf")).toList();
		assertThat(create(List.of(fiveImages.get(0), threeAttachments.get(0)))).isNotNull();

		assertError(() -> create(append(fiveImages, image("6.png"))), CommunityErrorCode.FILE_LIMIT_EXCEEDED);
		assertError(() -> create(append(threeAttachments, attachment("4.pdf"))), CommunityErrorCode.FILE_LIMIT_EXCEEDED);
	}

	@Test
	void 내가_발급받은_주소가_아닌_파일은_붙일_수_없다() {
		Long kim = TestUsers.approved(entityManager, "김사자");
		PostFileRequest others = new PostFileRequest(PostFileType.IMAGE, BASE_URL + kim + "/" + UUID.randomUUID() + ".png", "a.png", 100);
		PostFileRequest outside = new PostFileRequest(PostFileType.IMAGE, "https://example.com/a.png", "a.png", 100);

		assertError(() -> create(List.of(others)), FileErrorCode.INVALID_FILE_URL);
		assertError(() -> create(List.of(outside)), FileErrorCode.INVALID_FILE_URL);
	}

	private Long create(List<PostFileRequest> files) {
		return postService.create(me, new PostCreateRequest(PostCategory.MATERIAL, "족보 공유", "자료 올려요.",
				null, false, null, null, null, files)).id();
	}

	private PostFileRequest image(String name) {
		return new PostFileRequest(PostFileType.IMAGE, url(name), name, 1024);
	}

	private PostFileRequest attachment(String name) {
		return new PostFileRequest(PostFileType.ATTACHMENT, url(name), name, 2048);
	}

	private String url(String name) {
		return BASE_URL + me + "/" + UUID.randomUUID() + name.substring(name.lastIndexOf('.'));
	}

	private List<PostFileRequest> append(List<PostFileRequest> files, PostFileRequest file) {
		return Stream.concat(files.stream(), Stream.of(file)).toList();
	}

	private List<String> fileUrls(Long postId) {
		return postQueryService.getDetail(me, postId).files().stream().map(PostDetailResponse.FileResponse::url).toList();
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
