package com.lionlink.backend.domain.community.event;

import java.util.List;

public record MeetupInvitedEvent(Long postId, String title, String hostName, List<Long> inviteeIds) {
}
