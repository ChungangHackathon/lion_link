package com.lionlink.backend.domain.coffeechat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "coffee_chat_members")
@IdClass(CoffeeChatMemberId.class)
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoffeeChatMember {

	@Id
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "coffee_chat_id")
	private CoffeeChat coffeeChat;

	@Id
	private Long userId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private CoffeeChatMemberRole role;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime joinedAt;

	private LocalDateTime leftAt;

	@Builder
	private CoffeeChatMember(CoffeeChat coffeeChat, Long userId, CoffeeChatMemberRole role) {
		this.coffeeChat = coffeeChat;
		this.userId = userId;
		this.role = role;
	}
}
