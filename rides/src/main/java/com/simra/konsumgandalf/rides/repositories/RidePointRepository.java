package com.simra.konsumgandalf.rides.repositories;

import com.simra.konsumgandalf.common.models.entities.RidePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RidePointRepository extends JpaRepository<RidePoint, Long> {

	@Query(value = """
				SELECT r FROM RidePoint r
			    JOIN FETCH r.ride
			    WHERE r.ride.id = :rideId
			""")
	List<RidePoint> findByRideId(Long rideId);

}
