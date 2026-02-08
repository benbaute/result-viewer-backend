package com.simra.konsumgandalf.osmPlanet.classes.mapper;

import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.maps.TrafficTimesMapper;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TrafficTimesMapperTest {

	private static final ZoneId CET_ZONE = ZoneId.of("Europe/Berlin");

	public static Date createTime(int hour, int minute) {
		Random rand = new Random();

		int year = 2000 + rand.nextInt(100);
		int month = 0;
		int dayOfMonth = 1 + rand.nextInt(28);

		ZonedDateTime zonedDateTime = ZonedDateTime.of(year, month + 1, dayOfMonth, hour, minute, 0, 0, CET_ZONE);

		return Date.from(zonedDateTime.toInstant());
	}

	@Test
	public void testEveningNightMorning_Morning() {
		TrafficTimes expected = TrafficTimes.EVENING_NIGHT_MORNING;

		Date date = createTime(7, 29);

		TrafficTimes result = TrafficTimesMapper.getTrafficTime(date);

		assertEquals(expected, result);
	}

	@Test
	public void testEveningNightMorning_Evening() {
		TrafficTimes expected = TrafficTimes.EVENING_NIGHT_MORNING;

		Date date = createTime(19, 0);

		TrafficTimes result = TrafficTimesMapper.getTrafficTime(date);

		assertEquals(expected, result);
	}

	@Test
	public void testEarlyRushHour() {
		TrafficTimes expected = TrafficTimes.MORNING_RUSH_HOUR;

		Date date = createTime(7, 30);

		TrafficTimes result = TrafficTimesMapper.getTrafficTime(date);

		assertEquals(expected, result);
	}

	@Test
	public void testMidDay() {
		TrafficTimes expected = TrafficTimes.MID_DAY;

		Date date = createTime(10, 10);

		TrafficTimes result = TrafficTimesMapper.getTrafficTime(date);

		assertEquals(expected, result);
	}

	@Test
	public void testLateRushHour() {
		TrafficTimes expected = TrafficTimes.EVENING_RUSH_HOUR;

		Date date = createTime(15, 30);

		TrafficTimes result = TrafficTimesMapper.getTrafficTime(date);

		assertEquals(expected, result);
	}

}
