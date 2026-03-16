package com.simra.konsumgandalf.rides.classes.specifications;

import com.simra.konsumgandalf.common.models.entities.Ride;
import org.springframework.data.jpa.domain.Specification;

public class RideSpecifications {

	public static Specification<Ride> hasIdLike(Long id) {
		return (root, query, cb) -> id == null ? cb.conjunction() : cb.like(cb.toString(root.get("id")), id + "%");
	}

}
