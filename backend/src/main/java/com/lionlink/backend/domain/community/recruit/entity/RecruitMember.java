package com.lionlink.backend.domain.community.recruit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "recruit_members")
@IdClass(RecruitMemberId.class)
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitMember {

	@Id
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recruit_id")
	private CoffeeChatRecruit recruit;

	@Id
	private Long userId;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime joinedAt;

	@Builder
	private RecruitMember(CoffeeChatRecruit recruit, Long userId) {
		this.recruit = recruit;
		this.userId = userId;
	}
}
