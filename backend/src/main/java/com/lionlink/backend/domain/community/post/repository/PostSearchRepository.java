package com.lionlink.backend.domain.community.post.repository;

import com.lionlink.backend.domain.community.post.dto.request.PostSearchCondition;
import com.lionlink.backend.domain.community.post.entity.Post;
import com.lionlink.backend.domain.community.post.entity.PostCategory;
import com.lionlink.backend.domain.community.recruit.entity.RecruitStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class PostSearchRepository {

	private static final String RECRUITING = """
			 and ((p.category = :meetupCategory and exists (
			         select 1 from Meetup m
			         where m.postId = p.id and m.canceledAt is null and m.meetAt > :now
			           and (select count(mm) from MeetupMember mm where mm.meetup = m) < m.capacity))
			   or (p.category = :recruitCategory and exists (
			         select 1 from CoffeeChatRecruit r
			         where r.postId = p.id and r.status = :openStatus and r.deadline >= :today
			           and (select count(rm) from RecruitMember rm where rm.recruit = r) < r.capacity)))
			""";

	private final EntityManager entityManager;

	public Page<Post> search(PostSearchCondition condition, Pageable pageable) {
		Map<String, Object> params = new HashMap<>();
		String where = where(condition, params);
		String order = condition.sort() == PostSearchCondition.PostSort.VIEWS
				? " order by p.viewCount desc, p.id desc"
				: " order by p.createdAt desc, p.id desc";

		List<Post> posts = query("select p from Post p" + where + order, Post.class, params)
				.setFirstResult((int) pageable.getOffset())
				.setMaxResults(pageable.getPageSize())
				.getResultList();

		return PageableExecutionUtils.getPage(posts, pageable,
				() -> query("select count(p) from Post p" + where, Long.class, params).getSingleResult());
	}

	private String where(PostSearchCondition condition, Map<String, Object> params) {
		StringBuilder where = new StringBuilder(" where p.deletedAt is null");
		if (!CollectionUtils.isEmpty(condition.categories())) {
			where.append(" and p.category in :categories");
			params.put("categories", condition.categories());
		}
		if (StringUtils.hasText(condition.q())) {
			where.append(" and (lower(p.title) like :q escape '!' or lower(p.content) like :q escape '!')");
			params.put("q", "%" + escapeLike(condition.q().trim().toLowerCase(Locale.ROOT)) + "%");
		}
		if (condition.period() == PostSearchCondition.PostPeriod.WEEK) {
			where.append(" and p.createdAt >= :since");
			params.put("since", LocalDateTime.now().minusDays(7));
		}
		if (condition.recruiting()) {
			where.append(RECRUITING);
			params.put("meetupCategory", PostCategory.MEETUP);
			params.put("recruitCategory", PostCategory.RECRUIT);
			params.put("openStatus", RecruitStatus.OPEN);
			params.put("now", LocalDateTime.now());
			params.put("today", LocalDate.now());
		}
		return where.toString();
	}

	private <T> TypedQuery<T> query(String jpql, Class<T> type, Map<String, Object> params) {
		TypedQuery<T> query = entityManager.createQuery(jpql, type);
		params.forEach(query::setParameter);
		return query;
	}

	private String escapeLike(String value) {
		return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
	}
}
