package com.simra.konsumgandalf.rides.repositories;

import com.simra.konsumgandalf.common.models.entities.Ride;
import com.simra.konsumgandalf.rides.classes.specifications.RideSpecifications;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public interface RideRepository extends RepositoryPropertiesMappable<Ride, Long> {

	default Specification<Ride> createSpecification(Long id) {
		return Specification.where(RideSpecifications.hasIdLike(id));
	}

}
