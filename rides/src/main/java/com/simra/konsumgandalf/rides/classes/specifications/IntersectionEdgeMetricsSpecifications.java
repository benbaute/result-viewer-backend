package com.simra.konsumgandalf.rides.classes.specifications;

import com.simra.konsumgandalf.common.models.entities.IntersectionEdge;
import com.simra.konsumgandalf.common.models.entities.IntersectionEdgeMetrics;
import com.simra.konsumgandalf.common.models.entities.Region;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class IntersectionEdgeMetricsSpecifications {
    public static Specification<IntersectionEdgeMetrics> hasMinCount(Long count) {
        return (root, query, cb) ->
            count == null
                ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("numberOfRides"), count);
    }

    public static Specification<IntersectionEdgeMetrics> hasName(String name) {
        return (root, query, cb) ->
            name == null
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("name")),"%" + name.toLowerCase() + "%"
            );
    }

    public static Specification<IntersectionEdgeMetrics> hasRegion(
            String regionName
    ) {
        return (root, query, cb) -> {
            if (regionName == null) {
                return cb.conjunction();
            }

            assert query != null;
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<IntersectionEdge> edge = subquery.from(IntersectionEdge.class);

            Join<IntersectionEdge, Region> region = edge.join("regions");

            subquery.select(cb.literal(1L))
                    .where(
                            cb.equal(edge.get("id"), root.get("exampleId")),
                            cb.equal(region.get("name"), regionName)
                    );

            return cb.exists(subquery);
        };
    }

    public static Specification<IntersectionEdgeMetrics> hasWeekDay(List<WeekDays> weekDay) {
        return (root, query, cb) ->
                weekDay == null || weekDay.isEmpty() ?
                        cb.conjunction() : root.get("weekDay").in(weekDay);
    }

    public static Specification<IntersectionEdgeMetrics> hasTrafficTime(List<TrafficTimes> trafficTime) {
        return (root, query, cb) ->
                trafficTime == null || trafficTime.isEmpty() ?
                        cb.conjunction() : root.get("trafficTime").in(trafficTime);
    }

    public static Specification<IntersectionEdgeMetrics> hasYear(List<Integer> year) {
        return (root, query, cb) ->
                year == null || year.isEmpty() ?
                        cb.conjunction() : root.get("year").in(year);
    }
}
