package com.lionlink.backend.domain.user.repository;

import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.domain.user.entity.UserRole;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	boolean existsByIdAndStatusAndDeletedAtIsNull(Long id, UserStatus status);

	long countByIdInAndStatusAndDeletedAtIsNull(Collection<Long> ids, UserStatus status);

	boolean existsByIdAndRole(Long id, UserRole role);

	boolean existsByGoogleSub(String googleSub);

	Optional<User> findByGoogleSubAndDeletedAtIsNull(String googleSub);

	List<User> findAllByRoleAndDeletedAtIsNull(UserRole role);
}
