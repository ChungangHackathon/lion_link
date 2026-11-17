package com.lionlink.backend.domain.chat.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ChatRoomMemberId implements Serializable {

	// 필드명은 ChatRoomMember의 @Id 필드명과 같아야 한다
	private Long room;

	private Long userId;
}
