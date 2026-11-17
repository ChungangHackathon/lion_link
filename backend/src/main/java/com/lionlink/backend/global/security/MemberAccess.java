package com.lionlink.backend.global.security;

public record MemberAccess(boolean approved, boolean profileCompleted, boolean rejected) {

	public MemberAccess(boolean approved, boolean profileCompleted) {
		this(approved, profileCompleted, false);
	}
}
