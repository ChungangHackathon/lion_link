package com.lionlink.backend.domain.community.meetup.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MeetupMemberId implements Serializable {

	// 필드명은 MeetupMember의 @Id 필드명과 같아야 한다
	private Long meetup;

	private Long userId;
}
