package com.lionlink.backend.domain.coffeechat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "coffee_settings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoffeeSetting {

	@Id
	private Long userId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 10)
	private ReceiveMode defaultMode;

	private boolean acceptGroup;

	@JdbcTypeCode(SqlTypes.JSON)
	private List<String> topics;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private List<MeetingMode> modes;

	@JdbcTypeCode(SqlTypes.JSON)
	private List<AvailableSlot> availableSlots;

	@Column(length = 30)
	private String preferredArea;

	@Builder
	private CoffeeSetting(Long userId, List<String> topics, List<MeetingMode> modes,
			List<AvailableSlot> availableSlots, String preferredArea) {
		this.userId = userId;
		this.topics = topics;
		this.modes = modes;
		this.availableSlots = availableSlots;
		this.preferredArea = preferredArea;
		this.defaultMode = ReceiveMode.ON;
		this.acceptGroup = true;
	}

	/** PROFILE-13: 기본 상태를 뺀 설정을 보낸 값으로 통째로 바꾼다. */
	public void update(boolean acceptGroup, List<String> topics, List<MeetingMode> modes,
			List<AvailableSlot> availableSlots, String preferredArea) {
		this.acceptGroup = acceptGroup;
		this.topics = topics;
		this.modes = modes;
		this.availableSlots = availableSlots;
		this.preferredArea = preferredArea;
	}

	/** PROFILE-13·14: 기본 상태가 바뀌면 기간의 의미가 반대가 되므로, 부르는 쪽이 등록된 기간을 함께 지운다. */
	public void changeDefaultMode(ReceiveMode defaultMode) {
		this.defaultMode = defaultMode;
	}
}
