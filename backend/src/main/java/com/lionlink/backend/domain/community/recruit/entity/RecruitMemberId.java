package com.lionlink.backend.domain.community.recruit.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RecruitMemberId implements Serializable {

	// 필드명은 RecruitMember의 @Id 필드명과 같아야 한다
	private Long recruit;

	private Long userId;
}
