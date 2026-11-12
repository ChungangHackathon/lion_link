package com.lionlink.backend.domain.user.repository;

import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.domain.user.entity.UserRole;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

	boolean existsByIdAndDeletedAtIsNull(Long id);

	boolean existsByIdAndStatusAndDeletedAtIsNull(Long id, UserStatus status);

	long countByIdInAndStatusAndDeletedAtIsNull(Collection<Long> ids, UserStatus status);

	@Query("select u.id from User u where u.id in :ids and u.status = :status and u.deletedAt is null")
	Set<Long> findIdsByStatus(Collection<Long> ids, UserStatus status);

	boolean existsByIdAndRole(Long id, UserRole role);

	boolean existsByGoogleSub(String googleSub);

	Optional<User> findByGoogleSubAndDeletedAtIsNull(String googleSub);

	List<User> findAllByRoleAndDeletedAtIsNull(UserRole role);
}
