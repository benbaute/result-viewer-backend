package com.simra.konsumgandalf.rides.classes.specifications;

import com.simra.konsumgandalf.common.models.entities.IntersectionBase;
import com.simra.konsumgandalf.common.models.entities.IntersectionBaseMetrics;
import com.simra.konsumgandalf.common.models.entities.Region;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public class IntersectionBaseMetricsSpecifications {

	public static <T> Specification<T> hasMinCount(Long count) {
		return (root, query, cb) -> count == null ? cb.conjunction()
				: cb.greaterThanOrEqualTo(root.get("numberOfRides"), count);
	}

	public static <T extends IntersectionBaseMetrics> Specification<T> isInsideRegionPath(String targetLtreePath) {
		return (root, query, cb) -> {
			if (targetLtreePath == null || targetLtreePath.isEmpty()) {
				return cb.conjunction();
			}

			Join<IntersectionBase, Region> regionJoin = root.join("smallestRegion");

			return cb.isTrue(cb.function("str_to_ltree_is_ancestor", Boolean.class, regionJoin.get("ltreePath"),
					cb.literal(targetLtreePath)));
		};
	}

}
