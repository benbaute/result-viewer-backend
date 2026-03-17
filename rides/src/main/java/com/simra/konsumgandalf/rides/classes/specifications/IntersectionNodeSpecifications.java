package com.simra.konsumgandalf.rides.classes.specifications;

import com.simra.konsumgandalf.common.models.entities.IntersectionNode;
import com.simra.konsumgandalf.common.models.entities.TrafficSignalCluster;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public class IntersectionNodeSpecifications {

	public static Specification<IntersectionNode> hasTrafficSignalClusterId(Long trafficSignalClusterId) {
		return (root, query, cb) -> {
			if (trafficSignalClusterId == null) {
				return cb.conjunction();
			}

			Join<IntersectionNode, TrafficSignalCluster> tJoin = root.join("trafficSignalCluster");
			return cb.equal(tJoin.get("id"), trafficSignalClusterId);
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

}
