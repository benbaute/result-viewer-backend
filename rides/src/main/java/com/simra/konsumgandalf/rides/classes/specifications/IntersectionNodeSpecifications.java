package com.simra.konsumgandalf.rides.classes.specifications;

import com.simra.konsumgandalf.common.models.entities.IntersectionNode;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public class IntersectionNodeSpecifications {

	public static Specification<IntersectionNode> hasTrafficSignalClusterId(Long trafficSignalClusterId) {
		return (root, query, cb) -> {
			if (trafficSignalClusterId == null) {
				return cb.conjunction();
			}

			return cb.equal(root.get("trafficSignalCluster").get("id"), trafficSignalClusterId);
		};
	}

	public static Specification<IntersectionNode> isSegment(Long startValhallaEdgeId, Long endValhallaEdgeId) {
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

	public static Specification<IntersectionNode> fetchOsmLines() {
		return (root, query, cb) -> {
			// Only fetch if this is a data fetch query (skips count queries)
			if (query != null && Long.class != query.getResultType() && long.class != query.getResultType()) {
				root.fetch("startOsmLine", JoinType.LEFT);
				root.fetch("endOsmLine", JoinType.LEFT);
			}
			return null;
		};
	}

}
