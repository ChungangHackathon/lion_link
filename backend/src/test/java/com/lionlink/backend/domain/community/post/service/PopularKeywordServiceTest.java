package com.lionlink.backend.domain.community.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.lionlink.backend.domain.community.post.dto.response.PopularKeywordResponse.Item;
import com.lionlink.backend.domain.community.post.entity.BlockedKeyword;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PopularKeywordServiceTest {

	@Autowired
	private PopularKeywordService popularKeywordService;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 최근_7일_동안_검색한_사람이_많은_순서로_보여준다() {
		popularKeywordService.record(1L, "알고리즘");
		popularKeywordService.record(2L, "알고리즘");
		popularKeywordService.record(3L, "알고리즘");
		popularKeywordService.record(1L, "인턴");
		popularKeywordService.record(2L, "인턴");
		popularKeywordService.record(1L, "포트폴리오");
		popularKeywordService.record(1L, "알고리즘");
		popularKeywordService.record(1L, "포트폴리오");

		assertThat(popular(10)).extracting(Item::keyword, Item::count)
				.containsExactly(tuple("알고리즘", 3L), tuple("인턴", 2L), tuple("포트폴리오", 1L));
		assertThat(popular(2)).extracting(Item::keyword).containsExactly("알고리즘", "인턴");
	}

	@Test
	void 검색어의_공백과_대소문자를_정리하고_너무_짧거나_길면_세지_않는다() {
		popularKeywordService.record(1L, "  Spring   Boot ");
		popularKeywordService.record(2L, "spring boot");
		popularKeywordService.record(1L, "a");
		popularKeywordService.record(1L, "가".repeat(21));
		popularKeywordService.record(1L, "   ");
		popularKeywordService.record(1L, null);

		assertThat(popular(10)).extracting(Item::keyword, Item::count).containsExactly(tuple("spring boot", 2L));
	}

	@Test
	void 일주일이_지난_검색과_금칙어가_들어간_키워드는_빠진다() {
		popularKeywordService.record(1L, "중커톤");
		popularKeywordService.record(1L, "바보같은 질문");
		popularKeywordService.record(1L, "인턴");
		entityManager.persist(BlockedKeyword.builder().word("바보").build());
		entityManager.createNativeQuery("update post_search_keywords set searched_at = :old where keyword = '인턴'")
				.setParameter("old", LocalDateTime.now().minusDays(8))
				.executeUpdate();

		assertThat(popular(10)).extracting(Item::keyword).containsExactly("중커톤");
		assertThat(popularKeywordService.deleteExpired(LocalDateTime.now())).isEqualTo(1);
	}

	private List<Item> popular(int size) {
		return popularKeywordService.getPopular(size).items();
	}
}
