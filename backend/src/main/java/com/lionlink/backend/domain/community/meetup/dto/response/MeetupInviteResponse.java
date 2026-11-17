package com.lionlink.backend.domain.community.meetup.dto.response;

import java.util.List;

public record MeetupInviteResponse(int sentCount, List<Long> skippedUserIds) {
}
