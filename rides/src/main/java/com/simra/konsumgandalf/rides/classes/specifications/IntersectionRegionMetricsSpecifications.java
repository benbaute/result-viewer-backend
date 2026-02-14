package com.simra.konsumgandalf.rides.classes.specifications;

import com.simra.konsumgandalf.common.models.entities.IntersectionRegionMetrics;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import org.springframework.data.jpa.domain.Specification;


public class IntersectionRegionMetricsSpecifications {
    public static Specification<IntersectionRegionMetrics> hasRegionId(Long regionId) {
        return (root, query, cb) ->
            regionId == null
                ? cb.conjunction()
                : cb.equal(root.get("regionId"), regionId);
    }

    public static Specification<IntersectionRegionMetrics> hasMinCount(Long count) {
        return (root, query, cb) ->
            count == null
                ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("numberOfRides"), count);
    }

    public static Specification<IntersectionRegionMetrics> hasWeekDay(WeekDays weekDay) {
        return (root, query, cb) ->
                cb.equal(root.get("weekDay"), (weekDay.toString()));
    }

    public static Specification<IntersectionRegionMetrics> hasTrafficTime(TrafficTimes trafficTime) {
        return (root, query, cb) ->
                cb.equal(root.get("trafficTime"), (trafficTime.toString()));
    }

    public static Specification<IntersectionRegionMetrics> hasYear(Integer year) {
        return (root, query, cb) ->
                cb.equal(root.get("year"), (year));
    }
}
