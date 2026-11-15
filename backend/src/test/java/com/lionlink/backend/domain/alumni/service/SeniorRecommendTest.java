package com.lionlink.backend.domain.alumni.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lionlink.backend.domain.alumni.dto.response.AlumniResponse;
import com.lionlink.backend.domain.coffeechat.entity.ReceiveMode;
import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.support.TestProfiles;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class SeniorRecommendTest {

	@Autowired
	private AlumniService alumniService;

	@Autowired
	private EntityManager entityManager;

	private Long me;

	@BeforeEach
	void setUp() {
		me = alumni("나사자", 13, LionPart.BACKEND, List.of("BACKEND"), false);
	}

	@Test
	void 같은_파트_선배가_먼저_온다() {
		Long otherPart = alumni("기획선배", 12, LionPart.PLANNING, List.of("BACKEND"), true);
		Long samePart = alumni("백엔드선배", 12, LionPart.BACKEND, List.of("BACKEND"), true);

		List<Long> ids = ids(alumniService.recommendSeniors(me, 20));

		assertThat(ids).contains(samePart, otherPart);
		assertThat(ids.indexOf(samePart)).isLessThan(ids.indexOf(otherPart));
	}

	@Test
	void 파트가_같으면_관심_분야가_겹치는_선배가_먼저_온다() {
		Long noInterest = alumni("디자인관심선배", 12, LionPart.BACKEND, List.of("DESIGN"), true);
		Long sharedInterest = alumni("백엔드관심선배", 12, LionPart.BACKEND, List.of("BACKEND"), true);

		List<Long> ids = ids(alumniService.recommendSeniors(me, 20));

		assertThat(ids.indexOf(sharedInterest)).isLessThan(ids.indexOf(noInterest));
	}

	@Test
	void 선배가_없으면_랜덤으로_채운다() {
		Long peer = alumni("동기사자", 13, LionPart.BACKEND, List.of("BACKEND"), true);
		Long junior = alumni("후배사자", 14, LionPart.BACKEND, List.of("BACKEND"), true);

		List<Long> ids = ids(alumniService.recommendSeniors(me, 2));

		assertThat(ids).hasSize(2).doesNotContain(me);
		assertThat(ids).containsAnyOf(peer, junior);
	}

	@Test
	void 커피챗을_받지_않는_동문도_랜덤으로는_나온다() {
		Long resting = alumni("쉬는사자", 14, LionPart.BACKEND, List.of("BACKEND"), false);

		List<Long> ids = ids(alumniService.recommendSeniors(me, 2));

		assertThat(ids).contains(resting);
	}

	@Test
	void 추천에_나를_넣지_않는다() {
		alumni("백엔드선배", 12, LionPart.BACKEND, List.of("BACKEND"), true);

		assertThat(ids(alumniService.recommendSeniors(me, 20))).doesNotContain(me);
	}

	private List<Long> ids(List<AlumniResponse> responses) {
		return responses.stream().map(AlumniResponse::id).toList();
	}

	private Long alumni(String name, int generation, LionPart part, List<String> interests, boolean receiving) {
		Long userId = TestUsers.approved(entityManager, name);
		entityManager.createNativeQuery("update users set generation = :generation where id = :id")
				.setParameter("generation", generation)
				.setParameter("id", userId)
				.executeUpdate();
		entityManager.clear();
		TestProfiles.save(entityManager, TestProfiles.profile(userId).lionPart(part).interests(interests));
		if (receiving) {
			TestProfiles.coffeeSetting(entityManager, userId, ReceiveMode.ON, true);
		}
		return userId;
	}
}
