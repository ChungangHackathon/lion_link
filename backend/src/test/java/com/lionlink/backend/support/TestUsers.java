package com.lionlink.backend.support;

import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.entity.UserStatus;
import jakarta.persistence.EntityManager;
import java.util.concurrent.atomic.AtomicInteger;

public final class TestUsers {

	private static final AtomicInteger SEQUENCE = new AtomicInteger();

	private TestUsers() {
	}

	public static Long approved(EntityManager entityManager, String name) {
		return create(entityManager, name, UserStatus.APPROVED);
	}

	public static Long create(EntityManager entityManager, String name, UserStatus status) {
		User user = User.builder()
				.email("user" + SEQUENCE.incrementAndGet() + "@test.com")
				.name(name)
				.generation(13)
				.build();
		entityManager.persist(user);
		entityManager.flush();
		entityManager.createNativeQuery("update users set status = :status where id = :id")
				.setParameter("status", status.name())
				.setParameter("id", user.getId())
				.executeUpdate();
		entityManager.clear();
		return user.getId();
	}

	public static void withdraw(EntityManager entityManager, Long userId) {
		entityManager.flush();
		entityManager.createNativeQuery("update users set deleted_at = current_timestamp where id = :id")
				.setParameter("id", userId)
				.executeUpdate();
		entityManager.clear();
	}
}
