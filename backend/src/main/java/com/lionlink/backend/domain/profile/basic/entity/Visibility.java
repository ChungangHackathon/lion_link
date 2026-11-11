package com.lionlink.backend.domain.profile.basic.entity;

public enum Visibility {
	PUBLIC,
	CONNECTED,
	PRIVATE;

	public boolean isVisibleTo(boolean owner, boolean connected) {
		return switch (this) {
			case PUBLIC -> true;
			case CONNECTED -> owner || connected;
			case PRIVATE -> owner;
		};
	}
}
