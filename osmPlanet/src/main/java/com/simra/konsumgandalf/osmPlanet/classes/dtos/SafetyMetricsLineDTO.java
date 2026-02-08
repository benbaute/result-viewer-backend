package com.simra.konsumgandalf.osmPlanet.classes.dtos;

import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;

public record SafetyMetricsLineDTO(Long id, String name, String highway, float dangerousScore, String dangerousColor,
		int numberOfRides, int numberOfIncidents, TrafficTimes trafficTime, WeekDays weekDay, Integer year) {

}
