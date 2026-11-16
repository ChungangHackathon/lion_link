package com.lionlink.backend.domain.profile.section.lionactivity;

public enum LionRole {
	LION,
	STAFF,
	PRESIDENT,
	PART_LEAD,
	ETC;

	/** 운영진 태그를 붙이는 역할: 운영진·대표·파트장. */
	public boolean isStaff() {
		return this == STAFF || this == PRESIDENT || this == PART_LEAD;
	}
}
