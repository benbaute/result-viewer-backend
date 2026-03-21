package com.simra.konsumgandalf.rides.classes.specifications;

import com.simra.konsumgandalf.common.models.entities.IntersectionNodeMetrics;
import org.springframework.data.jpa.domain.Specification;

public class IntersectionNodeMetricsSpecifications {

	public static Specification<IntersectionNodeMetrics> hasTrafficSignalClusterId(Long trafficSignalClusterId) {
		return (root, query, cb) -> trafficSignalClusterId == null ? cb.conjunction()
				: cb.equal(root.get("trafficSignalClusterId"), trafficSignalClusterId);
	}

	public static Specification<IntersectionNodeMetrics> hasName(String name) {
		return (root, query, cb) -> name == null ? cb.conjunction()
				: cb.like(cb.lower(root.get("streetNames")), "%" + name.toLowerCase() + "%");
	}

	public static Specification<IntersectionNodeMetrics> isSegment(Long startValhallaEdgeId, Long endValhallaEdgeId) {
		return (root, query, cb) -> {
			if (startValhallaEdgeId == null && endValhallaEdgeId == null) {
				return cb.conjunction();
			}

			return cb.and(
					startValhallaEdgeId == null ? cb.isNull(root.get("startValhallaEdgeId"))
							: cb.equal(root.get("startValhallaEdgeId"), startValhallaEdgeId),

					endValhallaEdgeId == null ? cb.isNull(root.get("endValhallaEdgeId"))
							: cb.equal(root.get("endValhallaEdgeId"), endValhallaEdgeId));
		};
	}

}
