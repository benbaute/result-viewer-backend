package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@MappedSuperclass
public class TimeBaseClass {

    @Column(name = "traffic_time", length = 21)
    @Enumerated(EnumType.STRING)
    private TrafficTimes trafficTime;

    @Column(name = "week_day", length = 12)
    @Enumerated(EnumType.STRING)
    private WeekDays weekDay;

    @Column(name = "year")
    private Integer year;

	protected TimeBaseClass() {
	}

	protected TimeBaseClass(TrafficTimes trafficTime, WeekDays weekDay, Integer year) {
		this.trafficTime = trafficTime;
		this.weekDay = weekDay;
		this.year = year;
	}

    public Map<String, Object> getBaseProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("trafficTime", trafficTime);
        properties.put("weekDay", weekDay);
        properties.put("year", year);
        return properties;
    }
}
