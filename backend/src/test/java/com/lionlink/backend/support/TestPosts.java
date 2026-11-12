package com.lionlink.backend.support;

import com.lionlink.backend.domain.coffeechat.entity.CoffeeSetting;
import com.lionlink.backend.domain.coffeechat.entity.MeetingMode;
import com.lionlink.backend.domain.community.post.dto.request.PostCreateRequest;
import com.lionlink.backend.domain.community.post.entity.PostCategory;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class TestPosts {

	private TestPosts() {
	}

	public static PostCreateRequest general(PostCategory category, String title) {
		return new PostCreateRequest(category, title, "본문 내용입니다.", null, false, null, null, null);
	}

	public static PostCreateRequest meetup(String title, int capacity, List<Long> companionIds) {
		return new PostCreateRequest(PostCategory.MEETUP, title, "같이 저녁 먹어요.", null, false, null,
				new PostCreateRequest.MeetupRequest(LocalDateTime.now().plusDays(2), "흑석역 2번 출구", capacity, companionIds),
				null);
	}

	public static PostCreateRequest recruit(String title, Long seniorId, int capacity) {
		return new PostCreateRequest(PostCategory.RECRUIT, title, "함께 신청할 동문을 찾아요.", null, false, null, null,
				new PostCreateRequest.RecruitRequest(seniorId, capacity, LocalDate.now().plusDays(3)));
	}

	public static void acceptGroupCoffeeChat(EntityManager entityManager, Long userId) {
		entityManager.persist(CoffeeSetting.builder().userId(userId).modes(List.of(MeetingMode.ONLINE)).build());
		entityManager.flush();
		entityManager.clear();
	}

	public static void update(EntityManager entityManager, String sql, Long postId) {
		entityManager.flush();
		entityManager.createNativeQuery(sql).setParameter("id", postId).executeUpdate();
		entityManager.clear();
	}
}
