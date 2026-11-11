package com.lionlink.backend.domain.alumni.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionlink.backend.domain.alumni.dto.response.BookmarkedAlumniResponse;
import com.lionlink.backend.domain.alumni.exception.AlumniErrorCode;
import com.lionlink.backend.domain.alumni.repository.BookmarkRepository;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.response.SliceResponse;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class BookmarkServiceTest {

	@Autowired
	private BookmarkService bookmarkService;

	@Autowired
	private BookmarkRepository bookmarkRepository;

	@Autowired
	private EntityManager entityManager;

	private Long me;

	@BeforeEach
	void setUp() {
		me = TestUsers.approved(entityManager, "나사자");
	}

	@Test
	void 같은_동문을_여러_번_저장해도_한_번만_저장된다() {
		Long target = TestUsers.approved(entityManager, "김사자");

		bookmarkService.bookmark(me, target);
		bookmarkService.bookmark(me, target);

		assertThat(bookmarkRepository.count()).isEqualTo(1);
	}

	@Test
	void 자기_자신은_저장할_수_없다() {
		assertThatThrownBy(() -> bookmarkService.bookmark(me, me))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(AlumniErrorCode.CANNOT_BOOKMARK_SELF);
	}

	@Test
	void 없거나_승인되지_않았거나_탈퇴한_회원은_저장할_수_없다() {
		Long pending = TestUsers.create(entityManager, "대기사자", UserStatus.PENDING);
		Long withdrawn = TestUsers.approved(entityManager, "탈퇴사자");
		TestUsers.withdraw(entityManager, withdrawn);

		for (Long target : new Long[] {pending, withdrawn, 999_999L}) {
			assertThatThrownBy(() -> bookmarkService.bookmark(me, target))
					.isInstanceOf(BusinessException.class)
					.extracting("errorCode")
					.isEqualTo(AlumniErrorCode.USER_NOT_FOUND);
		}
	}

	@Test
	void 저장하지_않은_동문을_취소해도_성공한다() {
		Long target = TestUsers.approved(entityManager, "김사자");

		assertThatCode(() -> bookmarkService.unbookmark(me, target)).doesNotThrowAnyException();
	}

	@Test
	void 목록은_최근_저장순이고_탈퇴한_동문은_개인정보_없이_표시된다() {
		Long first = TestUsers.approved(entityManager, "김사자");
		Long second = TestUsers.approved(entityManager, "이사자");
		bookmarkService.bookmark(me, first);
		bookmarkService.bookmark(me, second);
		TestUsers.withdraw(entityManager, first);

		SliceResponse<BookmarkedAlumniResponse> result = bookmarkService.getBookmarks(me, PageRequest.of(0, 20));

		assertThat(result.items()).extracting(BookmarkedAlumniResponse::userId).containsExactly(second, first);
		BookmarkedAlumniResponse withdrawn = result.items().get(1);
		assertThat(withdrawn.isDeleted()).isTrue();
		assertThat(withdrawn.name()).isNull();
		assertThat(withdrawn.generation()).isNull();
		assertThat(withdrawn.bookmarkedAt()).isNotNull();
		assertThat(result.items().get(0).name()).isEqualTo("이사자");
		assertThat(result.hasNext()).isFalse();
	}
}
