package com.lionlink.backend.domain.community.meetup.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lionlink.backend.domain.community.exception.CommunityErrorCode;
import com.lionlink.backend.domain.community.meetup.repository.MeetupMemberRepository;
import com.lionlink.backend.domain.community.post.service.PostService;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.support.TestPosts;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class MeetupConcurrencyTest {

	private static final int APPLICANTS = 5;

	@Autowired
	private MeetupService meetupService;

	@Autowired
	private PostService postService;

	@Autowired
	private MeetupMemberRepository meetupMemberRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private final List<Long> userIds = new ArrayList<>();
	private Long postId;

	@AfterEach
	void tearDown() {
		if (postId != null) {
			jdbcTemplate.update("delete from chat_messages where room_id in (select id from chat_rooms where meetup_id = ?)", postId);
			jdbcTemplate.update("delete from chat_room_members where room_id in (select id from chat_rooms where meetup_id = ?)", postId);
			jdbcTemplate.update("delete from chat_rooms where meetup_id = ?", postId);
			jdbcTemplate.update("delete from meetup_members where meetup_id = ?", postId);
			jdbcTemplate.update("delete from meetups where post_id = ?", postId);
			jdbcTemplate.update("delete from posts where id = ?", postId);
		}
		userIds.forEach(userId -> jdbcTemplate.update("delete from users where id = ?", userId));
	}

	@Test
	void 마지막_한_자리에_여러_명이_동시에_참여해도_한_명만_들어간다() throws Exception {
		Long host = transactionTemplate.execute(status -> TestUsers.approved(entityManager, "나사자"));
		userIds.add(host);
		for (int i = 0; i < APPLICANTS; i++) {
			int index = i;
			userIds.add(transactionTemplate.execute(status -> TestUsers.approved(entityManager, "참여사자" + index)));
		}
		postId = postService.create(host, TestPosts.meetup("한 자리 번개", 2, List.of())).id();

		ExecutorService executor = Executors.newFixedThreadPool(APPLICANTS);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<String>> results = new ArrayList<>();
		for (Long applicant : userIds.subList(1, userIds.size())) {
			results.add(executor.submit(() -> {
				start.await();
				try {
					meetupService.join(applicant, postId);
					return "JOINED";
				} catch (BusinessException e) {
					return e.getErrorCode().getCode();
				}
			}));
		}
		start.countDown();
		List<String> outcomes = new ArrayList<>();
		for (Future<String> result : results) {
			outcomes.add(result.get(10, TimeUnit.SECONDS));
		}
		executor.shutdown();

		assertThat(outcomes).filteredOn("JOINED"::equals).hasSize(1);
		assertThat(outcomes).filteredOn(CommunityErrorCode.MEETUP_FULL.getCode()::equals).hasSize(APPLICANTS - 1);
		assertThat(meetupMemberRepository.countByMeetupPostId(postId)).isEqualTo(2);
	}
}
