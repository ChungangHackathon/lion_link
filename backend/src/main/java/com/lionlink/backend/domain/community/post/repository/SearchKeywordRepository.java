package com.lionlink.backend.domain.community.post.repository;

import com.lionlink.backend.domain.community.post.entity.SearchKeyword;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SearchKeywordRepository extends JpaRepository<SearchKeyword, Long> {

	@Modifying
	@Query(value = """
			insert into post_search_keywords (user_id, keyword, searched_at) values (:userId, :keyword, :now)
			on duplicate key update searched_at = :now
			""", nativeQuery = true)
	void upsert(@Param("userId") Long userId, @Param("keyword") String keyword, @Param("now") LocalDateTime now);

	@Query("""
			select k.keyword as keyword, count(k) as total
			from SearchKeyword k
			where k.searchedAt >= :from
			  and not exists (select b.id from BlockedKeyword b where k.keyword like concat('%', lower(b.word), '%'))
			group by k.keyword
			order by count(k) desc, max(k.searchedAt) desc, k.keyword
			""")
	List<KeywordCount> findPopular(@Param("from") LocalDateTime from, Pageable pageable);

	@Modifying
	@Query("delete from SearchKeyword k where k.searchedAt < :before")
	int deleteSearchedBefore(@Param("before") LocalDateTime before);

	interface KeywordCount {

		String getKeyword();

		long getTotal();
	}
}
