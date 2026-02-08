package com.simra.konsumgandalf.common.models.maps;

import com.google.common.collect.ImmutableSortedMap;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

public class TrafficTimesMapper {

	private static final ZoneId BERLIN_ZONE = ZoneId.of("Europe/Berlin");

	private static final ImmutableSortedMap<LocalTime, TrafficTimes> TRAFFIC_TIME_MAP = ImmutableSortedMap.of(
			LocalTime.of(7, 29, 59), TrafficTimes.EVENING_NIGHT_MORNING, LocalTime.of(9, 59, 59),
			TrafficTimes.MORNING_RUSH_HOUR, LocalTime.of(15, 29, 59), TrafficTimes.MID_DAY, LocalTime.of(18, 59, 59),
			TrafficTimes.EVENING_RUSH_HOUR);

	public static TrafficTimes getTrafficTime(Date date) {
		LocalTime localTime = LocalTime.ofInstant(date.toInstant(), BERLIN_ZONE);

		for (Map.Entry<LocalTime, TrafficTimes> entry : TRAFFIC_TIME_MAP.entrySet()) {
			if (entry.getKey().isAfter(localTime) || entry.getKey().equals(localTime)) {
				return entry.getValue();
			}
		}
		// Between 0:00 and 7:30
		return TrafficTimes.EVENING_NIGHT_MORNING;
	}

}
