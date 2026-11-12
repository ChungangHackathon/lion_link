package com.lionlink.backend.domain.community.meetup.repository;

import com.lionlink.backend.domain.community.meetup.entity.MeetupInvite;
import java.util.Collection;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MeetupInviteRepository extends JpaRepository<MeetupInvite, Long> {

	@Query("select i.inviteeId from MeetupInvite i where i.meetup.postId = :postId and i.inviteeId in :userIds")
	Set<Long> findInviteeIds(Long postId, Collection<Long> userIds);
}
