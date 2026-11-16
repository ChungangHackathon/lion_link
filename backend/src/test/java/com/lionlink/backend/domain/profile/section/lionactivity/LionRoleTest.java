package com.lionlink.backend.domain.profile.section.lionactivity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LionRoleTest {

	@Test
	void 운영진_대표_파트장만_운영진이다() {
		assertThat(LionRole.STAFF.isStaff()).isTrue();
		assertThat(LionRole.PRESIDENT.isStaff()).isTrue();
		assertThat(LionRole.PART_LEAD.isStaff()).isTrue();
		assertThat(LionRole.LION.isStaff()).isFalse();
		assertThat(LionRole.ETC.isStaff()).isFalse();
	}
}
