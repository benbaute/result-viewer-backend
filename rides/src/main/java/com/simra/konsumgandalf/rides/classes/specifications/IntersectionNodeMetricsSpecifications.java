package com.simra.konsumgandalf.rides.classes.specifications;

import com.simra.konsumgandalf.common.models.entities.IntersectionNode;
import com.simra.konsumgandalf.common.models.entities.IntersectionNodeMetrics;
import com.simra.konsumgandalf.common.models.entities.Region;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;


public class IntersectionNodeMetricsSpecifications {
    public static Specification<IntersectionNodeMetrics> hasTrafficSignalClusterId(Long trafficSignalClusterId) {
        return (root, query, cb) ->
            trafficSignalClusterId == null
                ? cb.conjunction()
                : cb.equal(root.get("trafficSignalClusterId"), trafficSignalClusterId);
    }

    public static Specification<IntersectionNodeMetrics> hasMinCount(Long count) {
        return (root, query, cb) ->
            count == null
                ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("numberOfRides"), count);
    }

    public static Specification<IntersectionNodeMetrics> hasName(String name) {
        return (root, query, cb) ->
            name == null
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("streetNames")),"%" + name.toLowerCase() + "%"
            );
    }

    public static Specification<IntersectionNodeMetrics> hasRegion(
            String regionName
    ) {
        return (root, query, cb) -> {
            if (regionName == null) {
                return cb.conjunction();
            }

            assert query != null;
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<IntersectionNode> node = subquery.from(IntersectionNode.class);

            Join<IntersectionNode, Region> region = node.join("regions");

            subquery.select(cb.literal(1L))
                    .where(
                            cb.equal(node.get("id"), root.get("exampleId")),
                            cb.equal(region.get("name"), regionName)
                    );

            return cb.exists(subquery);
        };
    }

    public static Specification<IntersectionNodeMetrics> hasWeekDay(WeekDays weekDay) {
        return (root, query, cb) ->
                cb.equal(root.get("weekDay"), (weekDay.toString()));
    }

    public static Specification<IntersectionNodeMetrics> hasTrafficTime(TrafficTimes trafficTime) {
        return (root, query, cb) ->
                cb.equal(root.get("trafficTime"), (trafficTime.toString()));
    }

    public static Specification<IntersectionNodeMetrics> hasYear(Integer year) {
        return (root, query, cb) ->
                cb.equal(root.get("year"), (year));
    }
}
