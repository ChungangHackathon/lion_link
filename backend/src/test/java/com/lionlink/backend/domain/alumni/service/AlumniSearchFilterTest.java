package com.lionlink.backend.domain.alumni.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lionlink.backend.domain.alumni.dto.request.AlumniSearchCondition;
import com.lionlink.backend.domain.alumni.dto.request.AlumniSearchCondition.AlumniSort;
import com.lionlink.backend.domain.alumni.dto.response.AlumniResponse;
import com.lionlink.backend.domain.profile.ProfileStatus;
import com.lionlink.backend.support.TestProfiles;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AlumniSearchFilterTest {

	@Autowired
	private AlumniService alumniService;

	@Autowired
	private BookmarkService bookmarkService;

	@Autowired
	private EntityManager entityManager;

	private Long me;

	@BeforeEach
	void setUp() {
		me = alumni("나사자", ProfileStatus.EMPLOYED);
	}

	@Test
	void 재직_중인_동문만_회사를_보여준다() {
		Long employed = alumni("재직사자", ProfileStatus.EMPLOYED);
		TestProfiles.company(entityManager, employed, "예시회사", "백엔드 개발자");

		assertThat(find(employed).company()).isEqualTo("예시회사");
		assertThat(find(employed).jobTitle()).isEqualTo("백엔드 개발자");
	}

	@Test
	void 재학_중인_동문은_회사를_숨긴다() {
		Long student = alumni("재학사자", ProfileStatus.STUDENT);
		TestProfiles.company(entityManager, student, "인턴회사", "인턴");

		assertThat(find(student).company()).isNull();
	}

	@Test
	void 회사가_비공개면_회사만_숨기고_직무는_보여준다() {
		Long employed = alumni("비공개사자", ProfileStatus.EMPLOYED);
		TestProfiles.company(entityManager, employed, "비밀회사", "백엔드 개발자");
		TestProfiles.visibility(entityManager, employed, "company_visibility", "PRIVATE");

		AlumniResponse found = find(employed);

		assertThat(found.company()).isNull();
		assertThat(found.jobTitle()).isEqualTo("백엔드 개발자");
	}

	@Test
	void 창업한_동문은_회사를_보여준다() {
		Long founder = alumni("창업사자", ProfileStatus.FOUNDER);
		TestProfiles.company(entityManager, founder, "내회사", "대표");

		assertThat(find(founder).company()).isEqualTo("내회사");
	}

	@Test
	void 졸업한_동문은_회사를_숨긴다() {
		Long graduated = alumni("졸업사자", ProfileStatus.GRADUATED);
		TestProfiles.company(entityManager, graduated, "예시회사", "백엔드 개발자");

		assertThat(find(graduated).company()).isNull();
	}

	@Test
	void 한줄소개와_자기소개는_그대로_내려준다() {
		Long target = alumni("소개사자", ProfileStatus.EMPLOYED);

		AlumniResponse found = find(target);

		assertThat(found.headline()).isEqualTo("한 줄 소개예요");
		assertThat(found.bio()).isEqualTo("자기소개예요");
	}

	@Test
	void 즐겨찾기한_동문만_걸러_볼_수_있다() {
		Long saved = alumni("관심사자", ProfileStatus.EMPLOYED);
		Long other = alumni("보통사자", ProfileStatus.EMPLOYED);
		bookmarkService.bookmark(me, saved);

		List<Long> ids = search(true).stream().map(AlumniResponse::id).toList();

		assertThat(ids).containsExactly(saved);
		assertThat(search(false).stream().map(AlumniResponse::id).toList()).contains(saved, other);
	}

	@Test
	void 즐겨찾기가_없으면_빈_결과다() {
		alumni("보통사자", ProfileStatus.EMPLOYED);

		assertThat(search(true)).isEmpty();
	}

	private AlumniResponse find(Long targetUserId) {
		return search(false).stream()
				.filter(response -> response.id().equals(targetUserId))
				.findFirst()
				.orElseThrow();
	}

	private List<AlumniResponse> search(boolean bookmarked) {
		AlumniSearchCondition condition = new AlumniSearchCondition(null, null, null, null, null, false, bookmarked,
				AlumniSort.LATEST);
		return alumniService.search(me, condition, PageRequest.of(0, 50)).items();
	}

	private Long alumni(String name, ProfileStatus status) {
		Long userId = TestUsers.approved(entityManager, name);
		TestProfiles.save(entityManager, TestProfiles.profile(userId)
				.status(status)
				.headline("한 줄 소개예요")
				.bio("자기소개예요"));
		return userId;
	}
}
