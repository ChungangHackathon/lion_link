package com.lionlink.backend.domain.profile;

public enum ProfileStatus {
	STUDENT,
	LEAVE,
	GRADUATED,
	EMPLOYED,
	FOUNDER,
	GRAD_SCHOOL,
	ETC;

	public boolean isWorking() {
		return this == EMPLOYED || this == FOUNDER;
	}
}
