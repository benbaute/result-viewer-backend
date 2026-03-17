package com.simra.konsumgandalf.rides.classes.specifications;

import com.simra.konsumgandalf.common.models.entities.IntersectionBase;
import com.simra.konsumgandalf.common.models.entities.Region;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public class IntersectionBaseSpecifications {

	public static <T extends IntersectionBase> Specification<T> hasRegion(Long regionId) {
		return (root, query, cb) -> {
			if (regionId == null) {
				return cb.conjunction();
			}

			Join<IntersectionBase, Region> regionJoin = root.join("regions");
			return cb.equal(regionJoin.get("id"), regionId);
		};
	}

}
