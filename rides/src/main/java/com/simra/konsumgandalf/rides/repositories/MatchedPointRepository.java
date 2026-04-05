package com.simra.konsumgandalf.rides.repositories;

import com.simra.konsumgandalf.common.models.entities.MatchedPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchedPointRepository extends JpaRepository<MatchedPoint, Long> {

	List<MatchedPoint> findByRideId(Long rideId);

	@Query(value = """
			SELECT m FROM MatchedPoint m
			   JOIN m.intersections i
			   WHERE i.id = :intersectionBaseId
					""")
	List<MatchedPoint> findByIntersectionBaseId(Long intersectionBaseId);

}
