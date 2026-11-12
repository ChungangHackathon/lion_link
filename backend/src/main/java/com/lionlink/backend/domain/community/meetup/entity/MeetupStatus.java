package com.lionlink.backend.domain.community.meetup.entity;

import java.time.LocalDateTime;

public enum MeetupStatus {
	OPEN,
	FULL,
	ENDED,
	CANCELED;

	public static MeetupStatus of(Meetup meetup, long participantCount, LocalDateTime now) {
		if (meetup.isCanceled()) {
			return CANCELED;
		}
		if (meetup.isEnded(now)) {
			return ENDED;
		}
		return participantCount >= meetup.getCapacity() ? FULL : OPEN;
	}
}
