package com.simra.konsumgandalf.rides.classes.specifications;

import com.simra.konsumgandalf.common.models.entities.TimeBaseClass;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import org.springframework.data.jpa.domain.Specification;


public class TimeSpecifications {

    public static <T extends TimeBaseClass> Specification<T> hasWeekDay(WeekDays weekDay) {
        return (root, query, cb) ->
                cb.equal(root.get("weekDay"), (weekDay.toString()));
    }

    public static <T extends TimeBaseClass> Specification<T> hasTrafficTime(TrafficTimes trafficTime) {
        return (root, query, cb) ->
                cb.equal(root.get("trafficTime"), (trafficTime.toString()));
    }

    public static <T extends TimeBaseClass> Specification<T> hasYear(Integer year) {
        return (root, query, cb) ->
                cb.equal(root.get("year"), (year));
    }
}
