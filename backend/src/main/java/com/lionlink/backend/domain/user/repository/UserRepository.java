package com.lionlink.backend.domain.user.repository;

import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.domain.user.entity.UserRole;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

	/** ADMIN-01: 탈퇴하지 않은 회원을 상태별로. 정렬은 Pageable의 Sort로 넘긴다. */
	@Query("select u from User u where u.status = :status and u.deletedAt is null")
	Page<User> findSignups(UserStatus status, Pageable pageable);

	/**
	 * ADMIN-03: 승인 대기·반려 상태일 때만 승인으로 바꾼다. 반려 사유는 비운다.
	 * 조건부 갱신이라 두 관리자가 동시에 처리해도 먼저 반영된 쪽만 1을 돌려받고, 나머지는 0이다.
	 * 벌크 쿼리는 @LastModifiedDate가 동작하지 않아 updatedAt도 직접 쓴다.
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update User u
			set u.status = com.lionlink.backend.domain.user.entity.UserStatus.APPROVED,
			    u.reviewedBy = :adminId, u.reviewedAt = :now, u.rejectReason = null, u.updatedAt = :now
			where u.id = :userId and u.deletedAt is null
			  and u.status in (com.lionlink.backend.domain.user.entity.UserStatus.PENDING,
			                   com.lionlink.backend.domain.user.entity.UserStatus.REJECTED)
			""")
	int approve(Long userId, Long adminId, LocalDateTime now);

	/** ADMIN-04: 승인 대기 상태일 때만 반려로 바꾼다. 영향받은 행이 0이면 이미 처리된 것이다. */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update User u
			set u.status = com.lionlink.backend.domain.user.entity.UserStatus.REJECTED,
			    u.reviewedBy = :adminId, u.reviewedAt = :now, u.rejectReason = :reason, u.updatedAt = :now
			where u.id = :userId and u.deletedAt is null
			  and u.status = com.lionlink.backend.domain.user.entity.UserStatus.PENDING
			""")
	int reject(Long userId, Long adminId, LocalDateTime now, String reason);

	/** ADMIN-02 */
	long countByStatusAndDeletedAtIsNull(UserStatus status);

	/** ADMIN-01 '같은 이름·기수' 대조용: 이름 집합과 기수 집합에 걸리는 승인 회원. 쌍 일치는 호출하는 쪽에서 거른다. */
	@Query("""
			select new com.lionlink.backend.domain.user.repository.UserIdentity(u.id, u.name, u.generation)
			from User u
			where u.status = com.lionlink.backend.domain.user.entity.UserStatus.APPROVED
			  and u.deletedAt is null
			  and u.name in :names
			  and u.generation in :generations
			""")
	List<UserIdentity> findApprovedIdentities(Collection<String> names, Collection<Integer> generations);
}
