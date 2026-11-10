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
	private List<String> availableSlots;

	@Column(length = 30)
	private String preferredArea;

	@Builder
	private CoffeeSetting(Long userId, List<String> topics, List<MeetingMode> modes,
			List<String> availableSlots, String preferredArea) {
		this.userId = userId;
		this.topics = topics;
		this.modes = modes;
		this.availableSlots = availableSlots;
		this.preferredArea = preferredArea;
		this.defaultMode = ReceiveMode.ON;
		this.acceptGroup = true;
	}
}
