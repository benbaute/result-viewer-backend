package com.simra.konsumgandalf.rides.repositories;

import com.simra.konsumgandalf.common.models.entities.MatchedPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

	@Query(value = """
			 SELECT DISTINCT m FROM MatchedPoint m
			     JOIN FETCH m.ridePoint rp
			     JOIN m.intersections i
			     WHERE i.id = :id
			        OR i.id = (SELECT b.prevIntersectionId FROM IntersectionBase b WHERE b.id = :id)
			        OR i.id IN (SELECT next.id FROM IntersectionBase next WHERE next.prevIntersectionId = :id)
			""")
	List<MatchedPoint> findMatchedPointsForIntersectionAndNeighbors(@Param("id") Long id);

}
