package com.lionlink.backend.domain.community.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatStatus;
import com.lionlink.backend.domain.community.exception.CommunityErrorCode;
import com.lionlink.backend.domain.community.meetup.entity.MeetupStatus;
import com.lionlink.backend.domain.community.post.dto.request.PostCreateRequest;
import com.lionlink.backend.domain.community.post.dto.request.PostSearchCondition;
import com.lionlink.backend.domain.community.post.dto.request.PostSearchCondition.PostPeriod;
import com.lionlink.backend.domain.community.post.dto.request.PostSearchCondition.PostSort;
import com.lionlink.backend.domain.community.post.dto.response.CategoryCountResponse;
import com.lionlink.backend.domain.community.post.dto.response.PostDetailResponse;
import com.lionlink.backend.domain.community.post.dto.response.PostMemberResponse;
import com.lionlink.backend.domain.community.post.dto.response.PostSummaryResponse;
import com.lionlink.backend.domain.community.post.entity.PostCategory;
import com.lionlink.backend.domain.community.recruit.entity.RecruitStatus;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.response.PageResponse;
import com.lionlink.backend.support.TestPosts;
import com.lionlink.backend.support.TestProfiles;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PostQueryServiceTest {

	private static final PostSearchCondition ALL = new PostSearchCondition(null, null, PostSort.LATEST, false, null);

	@Autowired
	private PostQueryService postQueryService;

	@Autowired
	private PostService postService;

	@Autowired
	private PostLikeService postLikeService;

	@Autowired
	private EntityManager entityManager;

	private Long me;

	@BeforeEach
	void setUp() {
		me = TestUsers.approved(entityManager, "나사자");
		TestProfiles.save(entityManager, TestProfiles.profile(me).bio("백엔드 개발을 공부해요"));
	}

	@Test
	void 삭제된_글을_빼고_최신순으로_조회한다() {
		Long first = create(TestPosts.general(PostCategory.FREE, "첫 글"));
		Long second = create(TestPosts.general(PostCategory.CONTEST, "둘째 글"));
		Long deleted = create(TestPosts.general(PostCategory.FREE, "삭제 글"));
		postService.delete(me, deleted);

		PageResponse<PostSummaryResponse> result = search(ALL);

		assertThat(ids(result)).containsExactly(second, first);
		assertThat(result.totalElements()).isEqualTo(2);
	}

	@Test
	void 카테고리와_제목_본문_검색어로_찾고_조회순으로_정렬한다() {
		Long spring = create(new PostCreateRequest(PostCategory.FREE, "Spring 스터디", "같이 공부해요.", null, false, null, null, null, null));
		Long body = create(new PostCreateRequest(PostCategory.FREE, "스터디 모집", "spring boot 위주로 해요.", null, false, null, null, null, null));
		Long contest = create(TestPosts.general(PostCategory.CONTEST, "공모전"));
		TestPosts.update(entityManager, "update posts set view_count = 10 where id = :id", body);

		assertThat(ids(search(new PostSearchCondition(null, "SPRING", PostSort.LATEST, false, null)))).containsExactly(body, spring);
		assertThat(ids(search(new PostSearchCondition(List.of(PostCategory.CONTEST), null, PostSort.LATEST, false, null)))).containsExactly(contest);
		assertThat(ids(search(new PostSearchCondition(null, null, PostSort.VIEWS, false, null)))).first().isEqualTo(body);
		assertThat(ids(search(new PostSearchCondition(null, "100%", PostSort.LATEST, false, null)))).isEmpty();
	}

	@Test
	void 목록에_작성자와_댓글_수를_담고_익명_글은_작성자를_숨긴다() {
		Long named = create(TestPosts.general(PostCategory.FREE, "실명 글"));
		Long anonymous = create(new PostCreateRequest(PostCategory.FREE, "익명 글", "본문입니다.", null, true, null, null, null, null));
		entityManager.createNativeQuery("""
						insert into comments (post_id, author_id, content, is_anonymous, created_at)
						values (:id, :authorId, '댓글', false, current_timestamp)
						""")
				.setParameter("id", named)
				.setParameter("authorId", me)
				.executeUpdate();

		List<PostSummaryResponse> items = search(ALL).items();

		PostSummaryResponse namedItem = find(items, named);
		assertThat(namedItem.author().name()).isEqualTo("나사자");
		assertThat(namedItem.author().generation()).isEqualTo(13);
		assertThat(namedItem.commentCount()).isEqualTo(1);
		assertThat(find(items, anonymous).author()).isNull();
	}

	@Test
	void 모집_중_필터는_정원이_남고_진행_중인_번개와_커피챗_모집만_보여준다() {
		Long kim = TestUsers.approved(entityManager, "김사자");
		Long senior = TestUsers.approved(entityManager, "선배사자");
		TestPosts.acceptGroupCoffeeChat(entityManager, senior);
		Long open = create(TestPosts.meetup("모집 중 번개", 4, null));
		Long full = create(TestPosts.meetup("마감 번개", 2, List.of(kim)));
		Long ended = create(TestPosts.meetup("끝난 번개", 4, null));
		TestPosts.update(entityManager, "update meetups set meet_at = dateadd('HOUR', -1, current_timestamp) where post_id = :id", ended);
		Long canceled = create(TestPosts.meetup("취소 번개", 4, null));
		TestPosts.update(entityManager, "update meetups set canceled_at = current_timestamp where post_id = :id", canceled);
		Long openRecruit = create(TestPosts.recruit("모집 중 커피챗", senior, 4));
		Long applied = create(TestPosts.recruit("신청한 커피챗", senior, 4));
		TestPosts.update(entityManager, "update coffeechat_recruits set status = 'APPLIED' where post_id = :id", applied);
		Long expired = create(TestPosts.recruit("마감 지난 커피챗", senior, 4));
		TestPosts.update(entityManager, "update coffeechat_recruits set deadline = dateadd('DAY', -1, current_date) where post_id = :id", expired);
		create(TestPosts.general(PostCategory.FREE, "일반 글"));

		PageResponse<PostSummaryResponse> result = search(new PostSearchCondition(null, null, PostSort.LATEST, true, null));

		assertThat(ids(result)).containsExactly(openRecruit, open);
		List<PostSummaryResponse> all = search(ALL).items();
		assertThat(find(all, full).meetup().status()).isEqualTo(MeetupStatus.FULL);
		assertThat(find(all, ended).meetup().status()).isEqualTo(MeetupStatus.ENDED);
		assertThat(find(all, canceled).meetup().status()).isEqualTo(MeetupStatus.CANCELED);
		PostSummaryResponse recruit = find(all, openRecruit);
		assertThat(recruit.recruit().senior().name()).isEqualTo("선배사자");
		assertThat(recruit.recruit().memberCount()).isEqualTo(1);
		assertThat(recruit.recruit().status()).isEqualTo(RecruitStatus.OPEN);
		assertThat(find(all, applied).recruit().status()).isEqualTo(RecruitStatus.APPLIED);
		assertThat(find(all, expired).recruit().status()).isEqualTo(RecruitStatus.ENDED);
		assertThat(postQueryService.getDetail(kim, expired).recruit().status()).isEqualTo(RecruitStatus.ENDED);
	}

	@Test
	void 최근_7일_필터는_오래된_글을_뺀다() {
		Long recent = create(TestPosts.general(PostCategory.FREE, "최근 글"));
		Long old = create(TestPosts.general(PostCategory.FREE, "오래된 글"));
		TestPosts.update(entityManager, "update posts set created_at = dateadd('DAY', -8, current_timestamp) where id = :id", old);

		assertThat(ids(search(new PostSearchCondition(null, null, PostSort.VIEWS, false, PostPeriod.WEEK)))).containsExactly(recent);
	}

	@Test
	void 상세_조회는_24시간_안에_한_번만_조회수를_올린다() {
		Long postId = create(TestPosts.general(PostCategory.FREE, "자유 글"));
		Long kim = TestUsers.approved(entityManager, "김사자");

		assertThat(postQueryService.getDetail(kim, postId).viewCount()).isEqualTo(1);
		assertThat(postQueryService.getDetail(kim, postId).viewCount()).isEqualTo(1);
		assertThat(postQueryService.getDetail(me, postId).viewCount()).isEqualTo(2);
	}

	@Test
	void 상세_조회에_작성자_좋아요_내_글_여부를_담는다() {
		Long postId = create(new PostCreateRequest(PostCategory.CONTEST, "공모전", "본문입니다.",
				List.of("https://a.com"), false, null, null, null, null));
		Long kim = TestUsers.approved(entityManager, "김사자");
		postLikeService.like(kim, postId);

		PostDetailResponse mine = postQueryService.getDetail(me, postId);
		assertThat(mine.isMine()).isTrue();
		assertThat(mine.author().name()).isEqualTo("나사자");
		assertThat(mine.author().status()).isNotNull();
		assertThat(mine.author().bio()).isEqualTo("백엔드 개발을 공부해요");
		assertThat(mine.links()).containsExactly("https://a.com");
		assertThat(mine.likeCount()).isEqualTo(1);
		assertThat(mine.likedByMe()).isFalse();
		assertThat(mine.meetup()).isNull();
		assertThat(mine.recruit()).isNull();

		PostDetailResponse others = postQueryService.getDetail(kim, postId);
		assertThat(others.isMine()).isFalse();
		assertThat(others.likedByMe()).isTrue();
	}

	@Test
	void 번개_상세는_작성자를_먼저_보여주고_내_참여_여부를_담는다() {
		Long kim = TestUsers.approved(entityManager, "김사자");
		Long lee = TestUsers.approved(entityManager, "이사자");
		Long postId = create(TestPosts.meetup("번개", 4, List.of(kim)));

		PostDetailResponse.MeetupDetail meetup = postQueryService.getDetail(kim, postId).meetup();

		assertThat(meetup.members()).extracting(PostMemberResponse::id).containsExactly(me, kim);
		assertThat(meetup.members()).extracting(PostMemberResponse::isHost).containsExactly(true, false);
		assertThat(meetup.participantCount()).isEqualTo(2);
		assertThat(meetup.status()).isEqualTo(MeetupStatus.OPEN);
		assertThat(meetup.joinedByMe()).isTrue();
		assertThat(postQueryService.getDetail(lee, postId).meetup().joinedByMe()).isFalse();
	}

	@Test
	void 커피챗_모집_상세는_선배와_진행_중인_커피챗_여부를_담는다() {
		Long senior = TestUsers.approved(entityManager, "선배사자");
		Long kim = TestUsers.approved(entityManager, "김사자");
		TestPosts.acceptGroupCoffeeChat(entityManager, senior);
		Long postId = create(TestPosts.recruit("모집", senior, 4));
		TestProfiles.coffeeChat(entityManager, kim, senior, CoffeeChatStatus.PENDING);

		PostDetailResponse.RecruitDetail forKim = postQueryService.getDetail(kim, postId).recruit();
		assertThat(forKim.senior().name()).isEqualTo("선배사자");
		assertThat(forKim.senior().acceptGroup()).isTrue();
		assertThat(forKim.joinedByMe()).isFalse();
		assertThat(forKim.hasOpenCoffeeChatWithSenior()).isTrue();
		assertThat(forKim.members()).extracting(PostMemberResponse::id).containsExactly(me);

		PostDetailResponse.RecruitDetail forSenior = postQueryService.getDetail(senior, postId).recruit();
		assertThat(forSenior.isSenior()).isTrue();
		assertThat(forSenior.hasOpenCoffeeChatWithSenior()).isFalse();
		assertThat(postQueryService.getDetail(me, postId).recruit().joinedByMe()).isTrue();

		TestProfiles.coffeePeriod(entityManager, senior, LocalDate.now(), LocalDate.now());
		assertThat(postQueryService.getDetail(kim, postId).recruit().senior().acceptGroup()).isFalse();
	}

	@Test
	void 삭제된_글은_상세_조회할_수_없다() {
		Long postId = create(TestPosts.general(PostCategory.FREE, "자유 글"));
		postService.delete(me, postId);

		assertThatThrownBy(() -> postQueryService.getDetail(me, postId))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(CommunityErrorCode.POST_NOT_FOUND);
	}

	@Test
	void 카테고리별_글_수는_없는_카테고리도_0으로_센다() {
		create(TestPosts.general(PostCategory.FREE, "자유 1"));
		create(TestPosts.general(PostCategory.FREE, "자유 2"));
		Long deleted = create(TestPosts.general(PostCategory.CONTEST, "공모전"));
		postService.delete(me, deleted);

		CategoryCountResponse result = postQueryService.countByCategory();

		assertThat(result.total()).isEqualTo(2);
		assertThat(result.counts()).hasSize(PostCategory.values().length);
		assertThat(result.counts().get(PostCategory.FREE)).isEqualTo(2);
		assertThat(result.counts().get(PostCategory.CONTEST)).isZero();
	}

	@Test
	void 좋아요는_여러_번_눌러도_한_번만_센다() {
		Long postId = create(TestPosts.general(PostCategory.FREE, "자유 글"));

		postLikeService.like(me, postId);
		assertThat(postLikeService.like(me, postId).likeCount()).isEqualTo(1);
		assertThat(postLikeService.unlike(me, postId).likeCount()).isZero();
		assertThat(postLikeService.unlike(me, postId).liked()).isFalse();
	}

	private Long create(PostCreateRequest request) {
		Long postId = postService.create(me, request).id();
		entityManager.flush();
		entityManager.clear();
		return postId;
	}

	private PageResponse<PostSummaryResponse> search(PostSearchCondition condition) {
		return postQueryService.search(condition, PageRequest.of(0, 20));
	}

	private List<Long> ids(PageResponse<PostSummaryResponse> result) {
		return result.items().stream().map(PostSummaryResponse::id).toList();
	}

	private PostSummaryResponse find(List<PostSummaryResponse> items, Long postId) {
		return items.stream().filter(item -> item.id().equals(postId)).findFirst().orElseThrow();
	}
}
