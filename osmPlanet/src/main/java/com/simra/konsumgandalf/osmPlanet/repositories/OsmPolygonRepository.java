package com.simra.konsumgandalf.osmPlanet.repositories;

import com.simra.konsumgandalf.common.models.entities.PlanetOsmPolygon;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.osmPlanet.classes.dtos.RideEntityTotalDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OsmPolygonRepository extends JpaRepository<PlanetOsmPolygon, Long> {

	/**
	 * Returns the length of all rides in a region
	 */
	@Query("""
				SELECT SUM(ST_LENGTH_M(r.way)) AS totalDistance,
				SUM(1) AS totalRides
				FROM PlanetOsmPolygon b
				JOIN RideEntity r
				ON r.trafficTime IN :trafficTime
				AND r.weekDay IN :weekDay
				AND r.year IN :year
				AND ST_INTERSECTS(b.way, ST_Transform(r.way, 3857))
				WHERE b.osmId = :osmId
				AND b.boundary = 'administrative'
			""")
	RideEntityTotalDTO totalRidesByRegion(Long osmId, List<TrafficTimes> trafficTime, List<WeekDays> weekDay,
			List<Integer> year);

	@Query("""
			 			SELECT ST_AsBinary(ST_Transform(b.way, 4326))
			             FROM PlanetOsmPolygon b
				WHERE b.osmId = :osmId
				ORDER BY ST_AREA(b.way) DESC
			 		LIMIT 1
			""")
	byte[] getWayByOsmId(Long osmId);

}
