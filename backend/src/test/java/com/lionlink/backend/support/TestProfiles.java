package com.lionlink.backend.support;

import com.lionlink.backend.domain.coffeechat.entity.CoffeeChat;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatStatus;
import com.lionlink.backend.domain.coffeechat.entity.MeetingMode;
import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.ProfileStatus;
import com.lionlink.backend.domain.profile.basic.entity.Profile;
import com.lionlink.backend.domain.profile.section.career.Career;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

public final class TestProfiles {

	private TestProfiles() {
	}

	public static Profile.ProfileBuilder profile(Long userId) {
		return Profile.builder()
				.userId(userId)
				.lionPart1(LionPart.BACKEND)
				.status(ProfileStatus.EMPLOYED)
				.interests(List.of("BACKEND"))
				.skills(List.of("Spring"));
	}

	public static void save(EntityManager entityManager, Profile.ProfileBuilder builder) {
		entityManager.persist(builder.build());
		entityManager.flush();
		entityManager.clear();
	}

	public static void currentCareer(EntityManager entityManager, Long userId, String company, String title) {
		entityManager.persist(Career.builder()
				.userId(userId)
				.company(company)
				.title(title)
				.startYm(YearMonth.of(2024, 3))
				.isCurrent(true)
				.build());
		entityManager.flush();
		entityManager.clear();
	}

	public static void visibility(EntityManager entityManager, Long userId, String column, String visibility) {
		entityManager.flush();
		entityManager.createNativeQuery("update profiles set " + column + " = :visibility where user_id = :userId")
				.setParameter("visibility", visibility)
				.setParameter("userId", userId)
				.executeUpdate();
		entityManager.clear();
	}

	public static void coffeeChat(EntityManager entityManager, Long requesterId, Long receiverId, CoffeeChatStatus status) {
		CoffeeChat coffeeChat = CoffeeChat.builder()
				.requesterId(requesterId)
				.receiverId(receiverId)
				.topic("CAREER")
				.reason("이야기 나누고 싶어요.")
				.questions(List.of())
				.preferredMode(MeetingMode.ONLINE)
				.expiresAt(LocalDateTime.now().plusDays(7))
				.build();
		entityManager.persist(coffeeChat);
		entityManager.flush();
		boolean open = status == CoffeeChatStatus.PENDING || status == CoffeeChatStatus.ACCEPTED
				|| status == CoffeeChatStatus.SCHEDULED;
		entityManager.createNativeQuery("update coffee_chats set status = :status, open_flag = :openFlag where id = :id")
				.setParameter("status", status.name())
				.setParameter("openFlag", open ? 1 : null)
				.setParameter("id", coffeeChat.getId())
				.executeUpdate();
		entityManager.clear();
	}
}
