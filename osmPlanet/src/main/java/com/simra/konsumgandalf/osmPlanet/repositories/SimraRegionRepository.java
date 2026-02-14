package com.simra.konsumgandalf.osmPlanet.repositories;

import com.simra.konsumgandalf.common.models.entities.SimraRegion;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.osmPlanet.classes.dtos.RideEntityTotalDTO;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface SimraRegionRepository extends JpaRepository<SimraRegion, Long> {

	Optional<SimraRegion> findByName(String name);

    @Modifying
    @Transactional
    @Query(value = """
UPDATE simra_region s
SET way = u.union_way
FROM (
    SELECT sr.simra_region_name,
           ST_ConvexHull(ST_Union(r.way)) AS union_way
    FROM region r
    JOIN simra_region__region sr
        ON sr.region_id = r.id
    GROUP BY sr.simra_region_name
) u
WHERE s.name = u.simra_region_name;
""", nativeQuery = true)
    void setSimraRegionGeometry();

	@Query("""
				SELECT SUM(ST_LENGTH_M(r.way)) AS totalDistance,
				SUM(1) AS totalRides
				FROM SimraRegion sr
				JOIN RideEntity r
				ON r.trafficTime IN :trafficTime
				AND r.weekDay IN :weekDay
				AND r.year IN :year
				AND ST_INTERSECTS(sr.way, ST_Transform(r.way, 3857))
				WHERE sr.name = :name
			""")
	RideEntityTotalDTO totalRides(String name, List<TrafficTimes> trafficTime, List<WeekDays> weekDay,
			List<Integer> year);

	@Query("""
				SELECT AVG(CAST(ST_LENGTH_M(ST_TRANSFORM(p.way, 4326)) AS double)) AS avgSegmentDistance
				FROM PlanetOsmLine p
				WHERE ST_INTERSECTS(:geo, ST_Transform(p.way, 3857))
			""")
	Float getAvgSegmentDistance(Geometry geo);

	@Query("SELECT r.name FROM SimraRegion sr JOIN sr.regions r WHERE sr.name = :name")
	List<String> findRegionNames(String name);

	@Query("SELECT s FROM SimraRegion s WHERE s.name = :name")
	Optional<SimraRegion> findWayByName(String name);
}
