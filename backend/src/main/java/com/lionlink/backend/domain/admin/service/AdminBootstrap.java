package com.lionlink.backend.domain.admin.service;

import com.lionlink.backend.domain.admin.config.AdminProperties;
import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.entity.UserRole;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.domain.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * app.admin.emails에 적힌 회원을 서버가 뜰 때 관리자로 올린다.
 * 첫 관리자는 승인해 줄 관리자가 없어 화면으로 만들 수 없고, 권한을 주는 일은 서버 설정을 만질 수 있는 사람만 해야 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

	private final AdminProperties properties;
	private final UserRepository userRepository;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		properties.emails().forEach(this::grant);
	}

	private void grant(String email) {
		List<User> users = userRepository.findAllByEmailAndDeletedAtIsNull(email);
		if (users.isEmpty()) {
			log.warn("app.admin.emails: 아직 가입하지 않은 이메일이라 건너뛴다. 가입한 뒤 다시 띄우면 지정된다. email={}", email);
			return;
		}
		for (User user : users) {
			if (user.getRole() == UserRole.ADMIN && user.getStatus() == UserStatus.APPROVED) {
				continue;
			}
			user.grantAdmin();
			log.info("app.admin.emails: 관리자로 지정했다. userId={}", user.getId());
		}
	}
}
