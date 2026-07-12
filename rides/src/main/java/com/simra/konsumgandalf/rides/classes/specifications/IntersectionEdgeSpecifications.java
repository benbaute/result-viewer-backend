package com.simra.konsumgandalf.rides.classes.specifications;

import com.simra.konsumgandalf.common.models.entities.IntersectionEdge;
import com.simra.konsumgandalf.common.models.entities.IntersectionNode;
import com.simra.konsumgandalf.common.models.entities.PlanetOsmLine;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public class IntersectionEdgeSpecifications {

	public static Specification<IntersectionEdge> hasOsmId(Long osmId) {
		return (root, query, cb) -> {
			if (osmId == null) {
				return cb.conjunction();
			}

			Join<IntersectionNode, PlanetOsmLine> lineJoin = root.join("osmLine");
			return cb.equal(lineJoin.get("id"), osmId);
		};
	}

	public static Specification<IntersectionEdge> isSegment(Long valhallaEdgeId, Long prevValhallaEdgeId,
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

	public static Specification<IntersectionEdge> fetchOsmLines() {
		return (root, query, cb) -> {
			// Only fetch if this is a data fetch query (skips count queries)
			if (query != null && Long.class != query.getResultType() && long.class != query.getResultType()) {
				root.fetch("osmLine", JoinType.LEFT);
				root.fetch("nextOsmLine", JoinType.LEFT);
				root.fetch("prevOsmLine", JoinType.LEFT);
			}
			return null;
		};
	}

}
