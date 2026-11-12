package com.lionlink.backend.domain.community.post.entity;

public enum PostCategory {
	CONTEST,
	JOB,
	MATERIAL,
	EVENT,
	NOW,
	FREE,
	MEETUP,
	RECRUIT;

	public boolean isGeneral() {
		return this != MEETUP && this != RECRUIT;
	}

	public boolean allowsDeadline() {
		return this == CONTEST || this == JOB;
	}

	public boolean allowsAnonymous() {
		return this == FREE;
	}
}
