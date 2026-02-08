package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public class TimeBaseClass {

	@Column(length = 21)
	@Enumerated(EnumType.STRING)
	private TrafficTimes trafficTime;

	@Column(length = 12)
	@Enumerated(EnumType.STRING)
	private WeekDays weekDay;

	@Column
	private Integer year;

	protected TimeBaseClass() {
	}

	protected TimeBaseClass(TrafficTimes trafficTime, WeekDays weekDay, Integer year) {
		this.trafficTime = trafficTime;
		this.weekDay = weekDay;
		this.year = year;
	}
}
