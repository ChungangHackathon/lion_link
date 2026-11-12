package com.lionlink.backend.domain.alumni.repository;

import com.lionlink.backend.domain.alumni.dto.request.AlumniSearchCondition;
import com.lionlink.backend.domain.coffeechat.entity.ReceiveMode;
import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.basic.entity.Profile;
import com.lionlink.backend.domain.profile.basic.entity.Visibility;
import com.lionlink.backend.domain.user.entity.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
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
public class AlumniSearchRepository {

	private static final String FROM = " from User u join Profile p on p.userId = u.id";

	private final EntityManager entityManager;

	public Page<Row> search(AlumniSearchCondition condition, Pageable pageable) {
		Map<String, Object> params = new HashMap<>();
		String where = where(condition, params);

		List<Row> rows = query("select u.id as id, u.name as name, u.generation as generation, p as profile"
				+ FROM + where + " order by u.id desc", Tuple.class, params)
				.setFirstResult((int) pageable.getOffset())
				.setMaxResults(pageable.getPageSize())
				.getResultStream()
				.map(Row::from)
				.toList();

		return PageableExecutionUtils.getPage(rows, pageable,
				() -> query("select count(u)" + FROM + where, Long.class, params).getSingleResult());
	}

	public List<Row> findSeniors(Long userId, Integer generation, LionPart part, int limit) {
		String jpql = "select u.id as id, u.name as name, u.generation as generation, p as profile" + FROM + """
				 where u.status = :approved and u.deletedAt is null and u.id <> :userId and u.generation < :generation
				   and exists (select 1 from CoffeeSetting s where s.userId = u.id and s.defaultMode = :receiving)
				 order by case when p.lionPart1 = :part or p.lionPart2 = :part then 0 else 1 end, u.generation desc, u.id desc
				""";
		return entityManager.createQuery(jpql, Tuple.class)
				.setParameter("approved", UserStatus.APPROVED)
				.setParameter("userId", userId)
				.setParameter("generation", generation)
				.setParameter("receiving", ReceiveMode.ON)
				.setParameter("part", part)
				.setMaxResults(limit)
				.getResultStream()
				.map(Row::from)
				.toList();
	}

	private String where(AlumniSearchCondition condition, Map<String, Object> params) {
		StringBuilder where = new StringBuilder(" where u.status = :approved and u.deletedAt is null");
		params.put("approved", UserStatus.APPROVED);

		if (StringUtils.hasText(condition.q())) {
			where.append("""
					 and (lower(u.name) like :q escape '!'
					  or lower(p.headline) like :q escape '!'
					  or lower(cast(p.skills as String)) like :q escape '!'
					  or exists (select 1 from Career c
					             where c.userId = u.id and c.isCurrent = true
					               and (lower(c.title) like :q escape '!'
					                 or (p.companyVisibility = :publicVisibility and lower(c.company) like :q escape '!'))))
					""");
			params.put("q", "%" + escapeLike(condition.q().trim().toLowerCase(Locale.ROOT)) + "%");
			params.put("publicVisibility", Visibility.PUBLIC);
		}
		if (!CollectionUtils.isEmpty(condition.generations())) {
			where.append(" and u.generation in :generations");
			params.put("generations", condition.generations());
		}
		if (!CollectionUtils.isEmpty(condition.parts())) {
			where.append(" and (p.lionPart1 in :parts or p.lionPart2 in :parts)");
			params.put("parts", condition.parts());
		}
		if (!CollectionUtils.isEmpty(condition.statuses())) {
			where.append(" and p.status in :statuses");
			params.put("statuses", condition.statuses());
		}
		if (!CollectionUtils.isEmpty(condition.interests())) {
			List<String> interestConditions = new ArrayList<>();
			for (int i = 0; i < condition.interests().size(); i++) {
				interestConditions.add("cast(p.interests as String) like :interest" + i + " escape '!'");
				params.put("interest" + i, "%\"" + escapeLike(condition.interests().get(i)) + "\"%");
			}
			where.append(" and (").append(String.join(" or ", interestConditions)).append(")");
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

	public record Row(Long id, String name, Integer generation, Profile profile) {

		private static Row from(Tuple tuple) {
			return new Row(
					tuple.get("id", Long.class),
					tuple.get("name", String.class),
					tuple.get("generation", Integer.class),
					tuple.get("profile", Profile.class)
			);
		}
	}
}
