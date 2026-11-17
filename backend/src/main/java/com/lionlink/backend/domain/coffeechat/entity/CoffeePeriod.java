package com.lionlink.backend.domain.coffeechat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "coffee_periods")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoffeePeriod {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private LocalDate startDate;

	@Column(nullable = false)
	private LocalDate endDate;

	@Builder
	private CoffeePeriod(Long userId, LocalDate startDate, LocalDate endDate) {
		this.userId = userId;
		this.startDate = startDate;
		this.endDate = endDate;
	}

	/** PROFILE-14·15: 기간을 줄이거나 겹친 기간을 합칠 때 쓴다. */
	public void changeRange(LocalDate startDate, LocalDate endDate) {
		this.startDate = startDate;
		this.endDate = endDate;
	}
}
