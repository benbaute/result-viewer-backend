package com.simra.konsumgandalf.osmPlanet.classes.dtos;

import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.osmPlanet.classes.keys.TrafficTimeWeekDayKey;

public class FindNumberOfRidesWithinStreetSegmentInTimePeriodDTO extends TrafficTimeWeekDayKey {

	private int numberOfRides;

	public FindNumberOfRidesWithinStreetSegmentInTimePeriodDTO(Object[] row) {
		super(TrafficTimes.valueOf((String) row[0]), WeekDays.valueOf((String) row[1]), ((Number) row[2]).intValue());
		this.numberOfRides = ((Number) row[3]).intValue();
	}

	public FindNumberOfRidesWithinStreetSegmentInTimePeriodDTO(TrafficTimes trafficTime, WeekDays weekDay, Integer year,
			long numberOfRides) {
		super(trafficTime, weekDay, year);
		this.numberOfRides = (int) numberOfRides;
	}

	public int getNumberOfRides() {
		return numberOfRides;
	}

	public void setNumberOfRides(int numberOfRides) {
		this.numberOfRides = numberOfRides;
	}

	@Override
	public TrafficTimeWeekDayKey getTrafficTimeWeekDayKey() {
		return new TrafficTimeWeekDayKey(this.trafficTime, this.weekDay, this.getYear()); // Cast
																							// to
																							// parent
																							// class
	}

}
