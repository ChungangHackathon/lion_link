package com.lionlink.backend.domain.coffeechat.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CoffeeChatMemberId implements Serializable {

	// 필드명은 CoffeeChatMember의 @Id 필드명과 같아야 한다
	private Long coffeeChat;

	private Long userId;
}
