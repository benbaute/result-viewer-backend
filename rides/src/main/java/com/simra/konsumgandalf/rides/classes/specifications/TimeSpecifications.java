package com.simra.konsumgandalf.rides.classes.specifications;

import com.simra.konsumgandalf.common.models.entities.TimeBaseClass;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import org.springframework.data.jpa.domain.Specification;

import java.util.Date;

public class TimeSpecifications {

	public static <T extends TimeBaseClass> Specification<T> hasWeekDayMetrics(WeekDays weekDay) {
		return (root, query, cb) -> cb.equal(root.get("weekDay"), (weekDay.toString()));
	}

	public static <T extends TimeBaseClass> Specification<T> hasTrafficTimeMetrics(TrafficTimes trafficTime) {
		return (root, query, cb) -> cb.equal(root.get("trafficTime"), (trafficTime.toString()));
	}

	public static <T extends TimeBaseClass> Specification<T> hasYearMetrics(Integer year) {
		return (root, query, cb) -> cb.equal(root.get("year"), (year));
	}

	public static <T extends TimeBaseClass> Specification<T> hasWeekDayBase(WeekDays weekDay) {
		return (root, query, cb) -> weekDay == null || weekDay == WeekDays.ALL_WEEK ? cb.conjunction()
				: cb.equal(root.get("weekDay"), (weekDay.toString()));
	}

	public static <T extends TimeBaseClass> Specification<T> hasTrafficTimeBase(TrafficTimes trafficTime) {
		return (root, query, cb) -> trafficTime == null || trafficTime == TrafficTimes.ALL_DAY ? cb.conjunction()
				: cb.equal(root.get("trafficTime"), (trafficTime.toString()));
	}

	public static <T extends TimeBaseClass> Specification<T> hasYearBase(Integer year) {
		return (root, query, cb) -> year == null || year == 2000 ? cb.conjunction()
				: cb.equal(root.get("year"), (year));
	}

	public static <T extends TimeBaseClass> Specification<T> inDateRange(Date start, Date end) {
		return (root, query, cb) -> start == null || end == null ? cb.conjunction() : cb
			.and(cb.greaterThanOrEqualTo(root.get("startTime"), start), cb.lessThanOrEqualTo(root.get("endTime"), end));
	}

}
