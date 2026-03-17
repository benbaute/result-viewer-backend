package com.simra.konsumgandalf.rides.classes.specifications;

import com.simra.konsumgandalf.common.models.entities.IntersectionRideRegionMetrics;
import org.springframework.data.jpa.domain.Specification;

public class IntersectionRideRegionMetricsSpecifications {

	public static Specification<IntersectionRideRegionMetrics> hasRegionId(Long regionId) {
		return (root, query, cb) -> regionId == null ? cb.conjunction() : cb.equal(root.get("regionId"), regionId);
	}

}
