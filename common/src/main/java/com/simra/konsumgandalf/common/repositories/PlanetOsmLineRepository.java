package com.simra.konsumgandalf.common.repositories;

import com.simra.konsumgandalf.common.models.entities.PlanetOsmLine;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlanetOsmLineRepository extends JpaRepository<PlanetOsmLine, Long> {

	@Query(value = """
				SELECT *
			 FROM planet_osm_line
			 WHERE osm_id IN (:streetSegmentIds)
			   AND ST_DWithin(
			         way,
			         ST_Transform(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326), 3857),
			         200
			       )
			 ORDER BY way <-> ST_Transform(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326), 3857)
			 LIMIT 1;
			""", nativeQuery = true)
	PlanetOsmLine findClosestStreetSegments(@Param("streetSegmentIds") List<Long> streetSegmentIds,
			@Param("lng") double lng, @Param("lat") double lat);

	@Modifying
	@Query("UPDATE PlanetOsmLine p SET p.lastModified = :timestamp WHERE p.id IN :ids")
	void updateLastModifiedByIds(Collection<Long> ids, Instant timestamp);

	@Query("SELECT p.id FROM PlanetOsmLine p WHERE p.id IN :ids")
	List<Long> findExistingIds(@Param("ids") Collection<Long> ids);

}
