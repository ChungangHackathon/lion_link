package com.lionlink.backend.domain.user.service;

import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.security.MemberAccess;
import com.lionlink.backend.global.security.MemberAccessReader;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserMemberAccessReader implements MemberAccessReader {

	private final UserRepository userRepository;

	@Override
	public Optional<MemberAccess> find(Long userId) {
		return userRepository.findById(userId)
				.filter(user -> user.getDeletedAt() == null)
				.map(user -> new MemberAccess(
						user.getStatus() == UserStatus.APPROVED,
						user.isProfileCompleted(),
						user.getStatus() == UserStatus.REJECTED));
	}
}
