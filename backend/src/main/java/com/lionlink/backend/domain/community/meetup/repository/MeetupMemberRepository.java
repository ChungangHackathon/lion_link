package com.lionlink.backend.domain.community.meetup.repository;

import com.lionlink.backend.domain.community.meetup.entity.MeetupMember;
import com.lionlink.backend.domain.community.meetup.entity.MeetupMemberId;
import com.lionlink.backend.global.jpa.IdCount;
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

	@Query("select m.userId from MeetupMember m where m.meetup.postId = :postId and m.userId in :userIds")
	Set<Long> findMemberIds(Long postId, Collection<Long> userIds);
}
