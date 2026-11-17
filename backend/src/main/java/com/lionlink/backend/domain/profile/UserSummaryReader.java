package com.lionlink.backend.domain.profile;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSummaryReader {

	private static final String QUERY = """
			select u.id as id, u.name as name, u.generation as generation, u.deletedAt as deletedAt,
			       p.lionPart as lionPart, p.photoUrl as photoUrl, p.headline as headline
			from User u
			left join Profile p on p.userId = u.id
			where u.id in :userIds
			""";

	private final EntityManager entityManager;

	public Map<Long, UserSummary> getSummaries(Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return Map.of();
		}
		List<Tuple> rows = entityManager.createQuery(QUERY, Tuple.class)
				.setParameter("userIds", userIds)
				.getResultList();
		return rows.stream()
				.map(this::toSummary)
				.collect(Collectors.toMap(UserSummary::id, Function.identity()));
	}

	public Optional<UserSummary> getSummary(Long userId) {
		return Optional.ofNullable(getSummaries(List.of(userId)).get(userId));
	}

	private UserSummary toSummary(Tuple row) {
		Long id = row.get("id", Long.class);
		if (row.get("deletedAt") != null) {
			return UserSummary.withdrawn(id);
		}
		LionPart lionPart = row.get("lionPart", LionPart.class);
		List<LionPart> lionParts = lionPart == null ? List.of() : List.of(lionPart);
		return new UserSummary(
				id,
				row.get("name", String.class),
				row.get("generation", Integer.class),
				lionParts,
				row.get("photoUrl", String.class),
				row.get("headline", String.class),
				false
		);
	}
}
