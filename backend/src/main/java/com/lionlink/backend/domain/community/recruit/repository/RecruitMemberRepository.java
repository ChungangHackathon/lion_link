package com.lionlink.backend.domain.community.recruit.repository;

import com.lionlink.backend.domain.community.recruit.entity.RecruitMember;
import com.lionlink.backend.domain.community.recruit.entity.RecruitMemberId;
import com.lionlink.backend.domain.community.recruit.entity.RecruitStatus;
import com.lionlink.backend.global.jpa.IdCount;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RecruitMemberRepository extends JpaRepository<RecruitMember, RecruitMemberId> {

	@Query("""
			select m.recruit.postId as id, count(m) as count
			from RecruitMember m
			where m.recruit.postId in :postIds
			group by m.recruit.postId
			""")
	List<IdCount> countByPostIds(Collection<Long> postIds);

	List<RecruitMember> findByRecruitPostIdOrderByJoinedAtAscUserIdAsc(Long postId);

	long countByRecruitPostId(Long postId);

	void deleteByRecruitPostId(Long postId);

	@Query("""
			select rm from RecruitMember rm join fetch rm.recruit r join fetch r.post p
			where rm.userId = :userId and p.authorId <> :userId and r.status = :status and p.deletedAt is null
			""")
	List<RecruitMember> findJoined(Long userId, RecruitStatus status);
}
