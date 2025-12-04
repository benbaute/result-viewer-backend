package com.simra.konsumgandalf.osmPlanet.repositories;

import com.simra.konsumgandalf.common.models.entities.PlanetOsmLine;
import com.simra.konsumgandalf.common.repositories.PlanetOsmLineRepository;
import com.simra.konsumgandalf.osmPlanet.classes.dtos.FindNumberOfRidesWithinStreetSegmentInTimePeriodDTO;
import com.simra.konsumgandalf.osmPlanet.classes.dtos.RideEntityDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface OsmHighwayRepository extends PlanetOsmLineRepository {

	@Modifying
	@Query("UPDATE PlanetOsmLine p SET p.lastAnalysed = :timestamp WHERE p.id IN :ids")
	void updateLastAnalysedByIds(Collection<Long> ids, Instant timestamp);

	@Query(value = """
			    WITH transformed_point AS (
			                 SELECT ST_Transform(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326), 3857) AS pt
			             )
			             SELECT
			                 planet_osm_line.osm_id,
			                 ST_AsGeoJSON(ST_Transform(ST_Simplify(planet_osm_line.way, :tolerance), 4326)) as way,
			                 sm.dangerous_color
			             FROM
			                 public.planet_osm_line as pl
			                 WHERE pl.last_modified IS NOT NULL AND last_analysed IS NOT NULL
			             JOIN
			                 transformed_point
			                 ON planet_osm_line.way && ST_Buffer(transformed_point.pt, :distanceFilter)
			             LEFT JOIN
			                 safety_metrics_planet_osm_line AS sm
			                 ON planet_osm_line.osm_id = sm.planet_osm_line_osm_id
			                 AND sm.traffic_time = :trafficTime
			                 AND sm.week_day = :weekDay
			                 AND sm.year = :year
			                 AND sm.number_of_rides >= 5
			             WHERE
			                 planet_osm_line.last_analysed IS NOT NULL
			                 AND planet_osm_line.highway IN :roadTypes
			                 AND sm.dangerous_color IS NOT NULL;
			""", nativeQuery = true)
	List<Map<String, Object>> findHighways(@Param("longitude") double longitude, @Param("latitude") double latitude,
			@Param("distanceFilter") int distanceFilter, @Param("roadTypes") List<String> roadTypes,
			@Param("tolerance") double tolerance, @Param("trafficTime") String trafficTime,
			@Param("weekDay") String weekDay, @Param("year") int year);

	@EntityGraph(attributePaths = { "rideIncident" })
	@Query("""
				SELECT p
				FROM PlanetOsmLine p
				WHERE p.lastModified IS NOT NULL
				AND (p.lastAnalysed IS NULL OR p.lastModified > p.lastAnalysed)
			""")
	// AND (p.rideEntities IS NOT EMPTY OR p.rideIncident IS NOT EMPTY)
	List<PlanetOsmLine> findAllStreets(Pageable pageable);

	@Query("""
			    SELECT new com.simra.konsumgandalf.osmPlanet.classes.dtos.FindNumberOfRidesWithinStreetSegmentInTimePeriodDTO(
			        r.trafficTime, r.weekDay, r.year, COUNT(r.id)
			    )
			    FROM PlanetOsmLine p
			    JOIN p.rideEntities r
			    WHERE p.id = :osmId
			    GROUP BY r.weekDay, r.trafficTime, r.year
			""")
	List<FindNumberOfRidesWithinStreetSegmentInTimePeriodDTO> findNumberOfRidesWithinStreetSegmentInTimePeriod(
			Long osmId);

	@EntityGraph(attributePaths = { "rideIncident", "safetyMetricPlanetOsmLines" })
	Optional<PlanetOsmLine> findById(Long id);

	@Query("""
				SELECT r.rideStart as rideStart, r.rideEnd as rideEnd
				FROM PlanetOsmLine p
				JOIN p.rideEntities r
				WHERE p.id = :id
				AND r.rideStart >= :startTime
				AND r.rideEnd <= :endTime
			""")
	List<RideEntityDTO> findRideEntitiesTimeById(Long id, LocalDateTime startTime, LocalDateTime endTime);

	@Query(value = "SELECT name FROM find_names_with_prefix(:namePrefix)", nativeQuery = true)
	List<String> findAllHighwayNameStartingWith(String namePrefix);

	@Query(value = "SELECT osm_id FROM find_osm_ids_with_prefix(:idPrefix)", nativeQuery = true)
	List<String> findAllHighwayIdStartingWith(String idPrefix);

	@Query(value = """
			    SELECT p.osm_id as osm_id, ST_AsGeoJSON(ST_TRANSFORM(ST_Simplify(p.way, 5), 4326)) AS way, p.highway
			    FROM planet_osm_line p
			    WHERE p.last_modified IS NOT NULL AND p.last_analysed IS NOT NULL
			""", nativeQuery = true)
	List<Map<String, Object>> getGridRaw();

}
