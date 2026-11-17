package com.lionlink.backend.domain.community.meetup.repository;

import com.lionlink.backend.domain.community.meetup.entity.MeetupMember;
import com.lionlink.backend.domain.community.meetup.entity.MeetupMemberId;
import com.lionlink.backend.global.jpa.IdCount;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MeetupMemberRepository extends JpaRepository<MeetupMember, MeetupMemberId> {

	@Query("""
			select m.meetup.postId as id, count(m) as count
			from MeetupMember m
			where m.meetup.postId in :postIds
			group by m.meetup.postId
			""")
	List<IdCount> countByPostIds(Collection<Long> postIds);

	List<MeetupMember> findByMeetupPostIdOrderByJoinedAtAscUserIdAsc(Long postId);

	long countByMeetupPostId(Long postId);

	void deleteByMeetupPostId(Long postId);

	@Query("""
			select mm from MeetupMember mm join fetch mm.meetup m join fetch m.post p
			where mm.userId = :userId and p.authorId <> :userId and p.deletedAt is null
			  and m.canceledAt is null and m.meetAt > :now
			""")
	List<MeetupMember> findJoinedInProgress(Long userId, LocalDateTime now);

	@Query("select m.userId from MeetupMember m where m.meetup.postId = :postId and m.userId in :userIds")
	Set<Long> findMemberIds(Long postId, Collection<Long> userIds);
}
