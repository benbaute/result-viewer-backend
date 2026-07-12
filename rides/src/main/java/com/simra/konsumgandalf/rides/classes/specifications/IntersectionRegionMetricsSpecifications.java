package com.simra.konsumgandalf.rides.classes.specifications;

import com.simra.konsumgandalf.common.models.entities.IntersectionRegionMetrics;
import org.springframework.data.jpa.domain.Specification;

public class IntersectionRegionMetricsSpecifications {

	public static Specification<IntersectionRegionMetrics> hasRegionId(Long regionId) {
		return (root, query, cb) -> regionId == null ? cb.conjunction()
				: cb.equal(root.get("region").get("id"), regionId);
	}

	public static Specification<IntersectionRegionMetrics> hasAdminLevel(Integer adminLevel) {
		return (root, query, cb) -> adminLevel == null ? cb.conjunction()
				: cb.equal(root.get("region").get("adminLevel"), adminLevel);
	}

	public static Specification<IntersectionRegionMetrics> isInsideRegionPath(String targetLtreePath) {
		return (root, query, cb) -> targetLtreePath == null || targetLtreePath.isEmpty() ? cb.conjunction()
				: cb.isTrue(cb.function("str_to_ltree_is_ancestor", Boolean.class, root.get("region").get("ltreePath"),
						cb.literal(targetLtreePath)));
	}

}
