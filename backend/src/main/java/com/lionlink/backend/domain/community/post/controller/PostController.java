package com.lionlink.backend.domain.community.post.controller;

import com.lionlink.backend.domain.community.post.dto.request.PostCreateRequest;
import com.lionlink.backend.domain.community.post.dto.request.PostSearchCondition;
import com.lionlink.backend.domain.community.post.dto.request.PostSearchCondition.PostPeriod;
import com.lionlink.backend.domain.community.post.dto.request.PostSearchCondition.PostSort;
import com.lionlink.backend.domain.community.post.dto.request.PostUpdateRequest;
import com.lionlink.backend.domain.community.post.dto.response.CategoryCountResponse;
import com.lionlink.backend.domain.community.post.dto.response.PopularKeywordResponse;
import com.lionlink.backend.domain.community.post.dto.response.PostCreateResponse;
import com.lionlink.backend.domain.community.post.dto.response.PostDetailResponse;
import com.lionlink.backend.domain.community.post.dto.response.PostSummaryResponse;
import com.lionlink.backend.domain.community.post.dto.response.PostUpdateResponse;
import com.lionlink.backend.domain.community.post.entity.PostCategory;
import com.lionlink.backend.domain.community.post.service.PopularKeywordService;
import com.lionlink.backend.domain.community.post.service.PostQueryService;
import com.lionlink.backend.domain.community.post.service.PostService;
import com.lionlink.backend.global.error.ErrorResponse;
import com.lionlink.backend.global.response.PageResponse;
import com.lionlink.backend.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "커뮤니티 게시글")
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

	private final PostService postService;
	private final PostQueryService postQueryService;
	private final PopularKeywordService popularKeywordService;

	@Operation(summary = "게시글 목록 조회 및 검색",
			description = "recruiting=true면 모집 중인 번개·커피챗 모집 글만, period=WEEK면 최근 7일 글만 조회한다. "
					+ "첫 페이지에서 결과가 있는 검색어는 인기 키워드에 기록한다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "400", description = "잘못된 조회 조건 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping
	public PageResponse<PostSummaryResponse> search(
			@CurrentUser Long userId,
			@Parameter(description = "카테고리 (여러 개는 쉼표로, 예: CONTEST,JOB). 없으면 전체")
			@RequestParam(name = "category", required = false) List<PostCategory> categories,
			@Parameter(description = "제목·본문 검색어")
			@RequestParam(required = false)
			@Size(max = 50, message = "검색어는 50자 이하여야 합니다.") String q,
			@Parameter(description = "정렬 (LATEST: 최신순, VIEWS: 조회순)") @RequestParam(defaultValue = "LATEST") PostSort sort,
			@Parameter(description = "모집 중인 번개·커피챗 모집만") @RequestParam(defaultValue = "false") boolean recruiting,
			@Parameter(description = "기간 (WEEK: 최근 7일)") @RequestParam(required = false) PostPeriod period,
			@Parameter(description = "페이지 번호 (0부터)")
			@RequestParam(defaultValue = "0")
			@Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.") int page,
			@Parameter(description = "페이지 크기 (1~100)")
			@RequestParam(defaultValue = "20")
			@Min(value = 1, message = "페이지 크기는 1~100이어야 합니다.")
			@Max(value = 100, message = "페이지 크기는 1~100이어야 합니다.") int size
	) {
		PostSearchCondition condition = new PostSearchCondition(categories, q, sort, recruiting, period);
		PageResponse<PostSummaryResponse> response = postQueryService.search(condition, PageRequest.of(page, size));
		if (page == 0 && response.totalElements() > 0) {
			popularKeywordService.record(userId, q);
		}
		return response;
	}

	@Operation(summary = "카테고리별 게시글 수", description = "글이 없는 카테고리도 0으로 반환한다.")
	@ApiResponse(responseCode = "200")
	@GetMapping("/category-counts")
	public CategoryCountResponse countByCategory(@CurrentUser Long userId) {
		return postQueryService.countByCategory();
	}

	@Operation(summary = "인기 키워드",
			description = "최근 7일 동안 커뮤니티 검색에 많이 쓰인 키워드. 같은 회원이 여러 번 검색해도 한 번으로 세고, 금칙어가 들어간 키워드는 뺀다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "400", description = "잘못된 개수 (VALIDATION_ERROR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/popular-keywords")
	public PopularKeywordResponse getPopularKeywords(
			@CurrentUser Long userId,
			@Parameter(description = "개수 (1~20)")
			@RequestParam(defaultValue = "10")
			@Min(value = 1, message = "개수는 1~20이어야 합니다.")
			@Max(value = 20, message = "개수는 1~20이어야 합니다.") int size
	) {
		return popularKeywordService.getPopular(size);
	}

	@Operation(summary = "게시글 작성",
			description = "MEETUP이면 meetup, RECRUIT이면 recruit 정보가 필요하다. 익명은 FREE에서만 가능하다.")
	@ApiResponse(responseCode = "201")
	@ApiResponse(responseCode = "400", description = "입력값 오류 (VALIDATION_ERROR, ANONYMOUS_NOT_ALLOWED, INVALID_CATEGORY_DETAIL, INVALID_MEETUP_TIME, INVALID_COMPANIONS, INVALID_RECRUIT_SENIOR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "함께하는 동문 또는 신청할 사자 없음 (USER_NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "지금 여러 명 커피챗을 받지 않는 사자 (GROUP_COFFEE_CHAT_NOT_ALLOWED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping
	public ResponseEntity<PostCreateResponse> create(
			@CurrentUser Long userId,
			@Valid @RequestBody PostCreateRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED).body(postService.create(userId, request));
	}

	@Operation(summary = "게시글 상세 조회", description = "같은 회원이 24시간 안에 다시 조회하면 조회수를 올리지 않는다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "404", description = "게시글 없음 (POST_NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/{postId}")
	public PostDetailResponse getDetail(
			@CurrentUser Long userId,
			@Parameter(description = "게시글 ID") @PathVariable Long postId
	) {
		return postQueryService.getDetail(userId, postId);
	}

	@Operation(summary = "게시글 수정",
			description = "보낸 필드만 바꾼다. 작성자만 수정할 수 있다. 번개·커피챗 모집 글은 작성자 말고 참여자가 없을 때만 카테고리를 바꿀 수 있다.")
	@ApiResponse(responseCode = "200")
	@ApiResponse(responseCode = "400", description = "입력값 오류 (VALIDATION_ERROR, INVALID_CATEGORY_DETAIL, INVALID_MEETUP_TIME, INVALID_RECRUIT_SENIOR)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "403", description = "작성자가 아님 (NOT_OWNER)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "게시글 없음 (POST_NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "수정할 수 없는 상태 (CATEGORY_CHANGE_NOT_ALLOWED, MEETUP_CLOSED, RECRUIT_CLOSED, RECRUIT_SENIOR_LOCKED, CAPACITY_BELOW_MEMBERS, GROUP_COFFEE_CHAT_NOT_ALLOWED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PatchMapping("/{postId}")
	public PostUpdateResponse update(
			@CurrentUser Long userId,
			@Parameter(description = "게시글 ID") @PathVariable Long postId,
			@Valid @RequestBody PostUpdateRequest request
	) {
		return postService.update(userId, postId, request);
	}

	@Operation(summary = "게시글 삭제",
			description = "작성자 또는 관리자가 삭제할 수 있다. 번개 글을 삭제하면 번개도 취소된다.")
	@ApiResponse(responseCode = "204")
	@ApiResponse(responseCode = "403", description = "권한 없음 (NOT_OWNER)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "게시글 없음 (POST_NOT_FOUND)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "삭제할 수 없는 상태 (MEETUP_CLOSED, RECRUIT_CLOSED)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@DeleteMapping("/{postId}")
	public ResponseEntity<Void> delete(
			@CurrentUser Long userId,
			@Parameter(description = "게시글 ID") @PathVariable Long postId
	) {
		postService.delete(userId, postId);
		return ResponseEntity.noContent().build();
	}
}
