package com.lionlink.backend.domain.community.meetup.entity;

import com.lionlink.backend.global.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "meetup_invites")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetupInvite extends BaseCreatedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "meetup_id", nullable = false)
	private Meetup meetup;

	@Column(nullable = false)
	private Long inviterId;

	@Column(nullable = false)
	private Long inviteeId;

	@Builder
	private MeetupInvite(Meetup meetup, Long inviterId, Long inviteeId) {
		this.meetup = meetup;
		this.inviterId = inviterId;
		this.inviteeId = inviteeId;
	}
}
