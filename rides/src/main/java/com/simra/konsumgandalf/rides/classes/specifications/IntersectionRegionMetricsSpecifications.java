package com.simra.konsumgandalf.rides.classes.specifications;

import com.simra.konsumgandalf.common.models.entities.IntersectionRegionMetrics;
import org.springframework.data.jpa.domain.Specification;


public class IntersectionRegionMetricsSpecifications {
    public static Specification<IntersectionRegionMetrics> hasRegionId(Long regionId) {
        return (root, query, cb) ->
            regionId == null
                ? cb.conjunction()
                : cb.equal(root.get("regionId"), regionId);
    }

    public static Specification<IntersectionRegionMetrics> hasAdminLevel(Integer adminLevel) {
        return (root, query, cb) ->
            adminLevel == null
                ? cb.conjunction()
                : cb.equal(root.get("adminLevel"), adminLevel);
    }
}
