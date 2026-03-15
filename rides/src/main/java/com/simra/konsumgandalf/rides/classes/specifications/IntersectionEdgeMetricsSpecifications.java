package com.simra.konsumgandalf.rides.classes.specifications;

import com.simra.konsumgandalf.common.models.entities.IntersectionEdgeMetrics;
import org.springframework.data.jpa.domain.Specification;

public class IntersectionEdgeMetricsSpecifications {

	public static Specification<IntersectionEdgeMetrics> hasOsmId(Long osmId) {
		return (root, query, cb) -> osmId == null ? cb.conjunction() : cb.or(cb.equal(root.get("osmId"), osmId),
				cb.equal(root.get("prevOsmId"), osmId), cb.equal(root.get("nextOsmId"), osmId));
	}

	public static Specification<IntersectionEdgeMetrics> hasName(String name) {
		return (root, query, cb) -> name == null ? cb.conjunction()
				: cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
	}

}
