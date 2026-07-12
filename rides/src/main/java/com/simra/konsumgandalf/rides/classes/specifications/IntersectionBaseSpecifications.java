package com.simra.konsumgandalf.rides.classes.specifications;

import com.simra.konsumgandalf.common.models.entities.IntersectionBase;
import com.simra.konsumgandalf.common.models.entities.Region;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public class IntersectionBaseSpecifications {

	public static <T extends IntersectionBase> Specification<T> isInsideRegionPath(String targetLtreePath) {
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
