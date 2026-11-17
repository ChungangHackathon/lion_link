package com.lionlink.backend.domain.community.recruit.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lionlink.backend.domain.community.exception.CommunityErrorCode;
import com.lionlink.backend.domain.community.post.service.PostService;
import com.lionlink.backend.domain.community.recruit.repository.RecruitMemberRepository;
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
class RecruitConcurrencyTest {

	private static final int APPLICANTS = 5;

	@Autowired
	private RecruitService recruitService;

	@Autowired
	private PostService postService;

	@Autowired
	private RecruitMemberRepository recruitMemberRepository;

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
			jdbcTemplate.update("delete from notifications where target_id = ?", postId);
			jdbcTemplate.update("delete from recruit_members where recruit_id = ?", postId);
			jdbcTemplate.update("delete from coffeechat_recruits where post_id = ?", postId);
			jdbcTemplate.update("delete from posts where id = ?", postId);
		}
		userIds.forEach(userId -> {
			jdbcTemplate.update("delete from coffee_settings where user_id = ?", userId);
			jdbcTemplate.update("delete from users where id = ?", userId);
		});
	}

	@Test
	void 마지막_한_자리에_여러_명이_동시에_참여해도_한_명만_들어간다() throws Exception {
		Long author = transactionTemplate.execute(status -> TestUsers.approved(entityManager, "나사자"));
		userIds.add(author);
		Long senior = transactionTemplate.execute(status -> {
			Long userId = TestUsers.approved(entityManager, "선배사자");
			TestPosts.acceptGroupCoffeeChat(entityManager, userId);
			return userId;
		});
		userIds.add(senior);
		List<Long> applicants = new ArrayList<>();
		for (int i = 0; i < APPLICANTS; i++) {
			int index = i;
			applicants.add(transactionTemplate.execute(status -> TestUsers.approved(entityManager, "참여사자" + index)));
		}
		userIds.addAll(applicants);
		postId = postService.create(author, TestPosts.recruit("한 자리 모집", senior, 2)).id();

		ExecutorService executor = Executors.newFixedThreadPool(APPLICANTS);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<String>> results = new ArrayList<>();
		for (Long applicant : applicants) {
			results.add(executor.submit(() -> {
				start.await();
				try {
					recruitService.join(applicant, postId);
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
		assertThat(outcomes).filteredOn(CommunityErrorCode.RECRUIT_FULL.getCode()::equals).hasSize(APPLICANTS - 1);
		assertThat(recruitMemberRepository.countByRecruitPostId(postId)).isEqualTo(2);
	}
}
