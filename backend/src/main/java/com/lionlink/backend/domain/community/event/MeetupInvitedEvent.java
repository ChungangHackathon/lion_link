package com.lionlink.backend.domain.community.event;

import java.util.List;

public record MeetupInvitedEvent(Long postId, String title, Long hostId, List<Long> inviteeIds) {
}
