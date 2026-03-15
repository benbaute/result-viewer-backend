package com.simra.konsumgandalf.rides.repositories;

import com.simra.konsumgandalf.common.models.entities.RideEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface RideEntityRepository extends JpaRepository<RideEntity, Long> {

	@Query(value = """
				SELECT
					 array_agg(DISTINCT ST_AsGeoJSON(st_transform(r.way, 4326))) as visited_way,
			           array_agg(DISTINCT ST_AsGeoJSON(st_transform(pl.way, 4326))) AS assigned_ways,
			           array_agg(DISTINCT ST_AsGeoJSON(st_transform(pi.way, 4326))) AS incident_ways,
			        array_agg(DISTINCT JSON_BUILD_OBJECT('scary', ri.scary, 'lat', ri.lat, 'lng', ri.lng,'id', ri.id)::text) FILTER (WHERE ri.scary IS NOT NULL OR ri.lat IS NOT NULL OR ri.lng IS NOT NULL OR ri.id IS NOT NULL) AS incident_locations

			      FROM ride_entity r
			      JOIN ride_entity__planet_osm_line repol ON r.id = repol.ride_entities_id
			      JOIN planet_osm_line pl ON repol.planet_osm_lines_osm_id = pl.osm_id
			      LEFT JOIN ride_incident ri ON ri.ride_entity_id = r.id
			      LEFT JOIN planet_osm_line pi ON ri.planet_osm_line_osm_id = pi.osm_id
			      WHERE r.id = :rideId;
			""",
			nativeQuery = true)
	Map<String, String[]> findRideGeometries(long rideId);

	@Query("""
			SELECT r.path
			FROM RideEntity r
			WHERE r.path IN :paths
			""")
	List<String> findExistingPaths(@Param("paths") List<String> paths);

}
