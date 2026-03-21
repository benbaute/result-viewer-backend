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

	public static Specification<IntersectionEdgeMetrics> isSegment(Long valhallaEdgeId, Long prevValhallaEdgeId,
			Long nextValhallaEdgeId) {
		return (root, query, cb) -> {
			if (valhallaEdgeId == null && prevValhallaEdgeId == null && nextValhallaEdgeId == null) {
				return cb.conjunction();
			}

			return cb.and(
					prevValhallaEdgeId == null ? cb.isNull(root.get("prevValhallaEdgeId"))
							: cb.equal(root.get("prevValhallaEdgeId"), prevValhallaEdgeId),

					valhallaEdgeId == null ? cb.isNull(root.get("valhallaEdgeId"))
							: cb.equal(root.get("valhallaEdgeId"), valhallaEdgeId),

					nextValhallaEdgeId == null ? cb.isNull(root.get("nextValhallaEdgeId"))
							: cb.equal(root.get("nextValhallaEdgeId"), nextValhallaEdgeId));
		};
	}

}
