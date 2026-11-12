package com.lionlink.backend.domain.alumni.repository;

import com.lionlink.backend.domain.alumni.dto.request.AlumniSearchCondition;
import com.lionlink.backend.domain.alumni.dto.request.AlumniSearchCondition.AlumniSort;
import com.lionlink.backend.domain.coffeechat.entity.ReceiveMode;
import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.basic.entity.Profile;
import com.lionlink.backend.domain.profile.basic.entity.Visibility;
import com.lionlink.backend.domain.user.entity.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

	private static final String SELECT = "select u.id as id, u.name as name, u.generation as generation, p as profile";
	private static final String FROM = " from User u join Profile p on p.userId = u.id";
	private static final String ACTIVE = " u.status = :approved and u.deletedAt is null";
	private static final String IN_PERIOD = """
			exists (select 1 from CoffeePeriod cp
			        where cp.userId = u.id and cp.startDate <= :today and cp.endDate >= :today)""";
	private static final String RECEIVING = """
			exists (select 1 from CoffeeSetting s
			        where s.userId = u.id
			          and ((s.defaultMode = :modeOn and not %s) or (s.defaultMode = :modeOff and %s)))"""
			.formatted(IN_PERIOD, IN_PERIOD);
	private static final String BOOKMARKED_FROM = " from Bookmark b join User u on u.id = b.targetUserId"
			+ " join Profile p on p.userId = u.id where b.userId = :userId and" + ACTIVE;

	private final EntityManager entityManager;

	public Page<Row> search(Long userId, List<LionPart> userParts, AlumniSearchCondition condition, Pageable pageable) {
		Map<String, Object> params = new HashMap<>();
		String where = where(userId, condition, params);
		Map<String, Object> selectParams = new HashMap<>(params);
		String orderBy = orderBy(condition.sort(), userParts, selectParams);

		List<Row> rows = query(SELECT + FROM + where + orderBy, Tuple.class, selectParams)
				.setFirstResult((int) pageable.getOffset())
				.setMaxResults(pageable.getPageSize())
				.getResultStream()
				.map(Row::from)
				.toList();

		return PageableExecutionUtils.getPage(rows, pageable,
				() -> query("select count(u)" + FROM + where, Long.class, params).getSingleResult());
	}

	public List<Row> findSeniors(Long userId, Integer generation, LionPart part, int limit) {
		Map<String, Object> params = new HashMap<>(Map.of(
				"approved", UserStatus.APPROVED, "userId", userId, "generation", generation));
		putReceivingParams(params);
		String jpql = SELECT + FROM + " where" + ACTIVE + " and u.id <> :userId and u.generation < :generation and " + RECEIVING
				+ " order by case when p.lionPart1 = :part or p.lionPart2 = :part then 0 else 1 end, u.generation desc, u.id desc";
		params.put("part", part);

		return query(jpql, Tuple.class, params)
				.setMaxResults(limit)
				.getResultStream()
				.map(Row::from)
				.toList();
	}

	public Page<BookmarkedRow> findBookmarked(Long userId, Pageable pageable) {
		List<BookmarkedRow> rows = query(SELECT + ", b.createdAt as bookmarkedAt" + BOOKMARKED_FROM
				+ " order by b.createdAt desc, b.targetUserId desc", Tuple.class, bookmarkedParams(userId))
				.setFirstResult((int) pageable.getOffset())
				.setMaxResults(pageable.getPageSize())
				.getResultStream()
				.map(BookmarkedRow::from)
				.toList();

		return PageableExecutionUtils.getPage(rows, pageable, () -> countBookmarked(userId));
	}

	public long countBookmarked(Long userId) {
		return query("select count(b)" + BOOKMARKED_FROM, Long.class, bookmarkedParams(userId)).getSingleResult();
	}

	private String where(Long userId, AlumniSearchCondition condition, Map<String, Object> params) {
		StringBuilder where = new StringBuilder(" where" + ACTIVE + " and u.id <> :userId");
		params.put("approved", UserStatus.APPROVED);
		params.put("userId", userId);

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
		if (condition.available()) {
			where.append(" and ").append(RECEIVING);
			putReceivingParams(params);
		}
		return where.toString();
	}

	private String orderBy(AlumniSort sort, List<LionPart> userParts, Map<String, Object> params) {
		return switch (sort) {
			case LATEST -> " order by u.id desc";
			case GENERATION -> " order by u.generation asc, u.id desc";
			case RECOMMEND -> {
				putReceivingParams(params);
				String samePart = "";
				if (!userParts.isEmpty()) {
					samePart = " case when p.lionPart1 in :userParts or p.lionPart2 in :userParts then 0 else 1 end,";
					params.put("userParts", userParts);
				}
				yield " order by case when " + RECEIVING + " then 0 else 1 end," + samePart + " u.id desc";
			}
		};
	}

	private void putReceivingParams(Map<String, Object> params) {
		params.put("today", LocalDate.now());
		params.put("modeOn", ReceiveMode.ON);
		params.put("modeOff", ReceiveMode.OFF);
	}

	private Map<String, Object> bookmarkedParams(Long userId) {
		return Map.of("userId", userId, "approved", UserStatus.APPROVED);
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

	public record BookmarkedRow(Row row, LocalDateTime bookmarkedAt) {

		private static BookmarkedRow from(Tuple tuple) {
			return new BookmarkedRow(Row.from(tuple), tuple.get("bookmarkedAt", LocalDateTime.class));
		}
	}
}
