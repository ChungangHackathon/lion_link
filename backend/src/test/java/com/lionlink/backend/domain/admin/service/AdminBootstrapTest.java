package com.lionlink.backend.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.lionlink.backend.domain.admin.config.AdminProperties;
import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.entity.UserRole;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AdminBootstrapTest {

	private static final AtomicInteger SEQUENCE = new AtomicInteger();

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	private String email() {
		return "bootstrap" + SEQUENCE.incrementAndGet() + "@test.com";
	}

	private Long signup(String email) {
		User user = User.builder().email(email).name("정운영").generation(13).build();
		entityManager.persist(user);
		entityManager.flush();
		return user.getId();
	}

	private void bootstrap(String... emails) {
		new AdminBootstrap(new AdminProperties(List.of(emails)), userRepository).run(null);
		entityManager.flush();
		entityManager.clear();
	}

	private User reload(Long userId) {
		return userRepository.findById(userId).orElseThrow();
	}

	@Test
	void 승인_대기_회원을_관리자로_올린다() {
		String email = email();
		Long userId = signup(email);

		bootstrap(email);

		assertThat(reload(userId).getRole()).isEqualTo(UserRole.ADMIN);
		assertThat(reload(userId).getStatus()).isEqualTo(UserStatus.APPROVED);
	}

	@Test
	void 앞뒤_공백이_있어도_찾는다() {
		String email = email();
		Long userId = signup(email);

		bootstrap("  " + email + "  ");

		assertThat(reload(userId).getRole()).isEqualTo(UserRole.ADMIN);
	}

	@Test
	void 가입하지_않은_이메일은_건너뛴다() {
		assertThatCode(() -> bootstrap(email())).doesNotThrowAnyException();
	}

	@Test
	void 탈퇴한_회원은_올리지_않는다() {
		String email = email();
		Long userId = signup(email);
		TestUsers.withdraw(entityManager, userId);

		bootstrap(email);

		assertThat(reload(userId).getRole()).isEqualTo(UserRole.MEMBER);
	}

	@Test
	void 설정이_비어_있으면_아무도_올리지_않는다() {
		Long userId = signup(email());

		bootstrap();

		assertThat(reload(userId).getRole()).isEqualTo(UserRole.MEMBER);
	}

	@Test
	void 이미_관리자면_그대로_둔다() {
		String email = email();
		Long userId = signup(email);
		bootstrap(email);

		assertThatCode(() -> bootstrap(email)).doesNotThrowAnyException();

		assertThat(reload(userId).getRole()).isEqualTo(UserRole.ADMIN);
	}
}
