package com.simra.konsumgandalf.common.repositories;

import com.simra.konsumgandalf.common.logging.LogExecutionTimeSubTask;
import com.simra.konsumgandalf.common.models.entities.PlanetOsmLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PlanetOsmLineRepository extends JpaRepository<PlanetOsmLine, Long> {

    @LogExecutionTimeSubTask
    @Query(value = """
    WITH input_numbers AS (
        SELECT
            unnest(cast(:incidentIds as bigint[])) as input_id,
            unnest(cast(:lngs as float[])) as input_lng,
            unnest(cast(:lats as float[])) as input_lat
    ),
    input_points AS (
        SELECT
            input_id,
            ST_Transform(ST_SetSRID(ST_MakePoint(input_lng, input_lat), 4326), 3857) AS input_point
        FROM input_numbers
    )
    SELECT
        ip.input_id as incidentId,
        l.osm_id
    FROM input_points ip
    CROSS JOIN LATERAL (
        SELECT pol.osm_id
        FROM planet_osm_line pol
        WHERE pol.osm_id IN (:streetSegmentIds)
          AND ST_DWithin(pol.way, ip.input_point, 200)
        ORDER BY pol.way <-> ip.input_point
        LIMIT 1
    ) l
""", nativeQuery = true)
    List<Long[]> findClosestStreetSegments(
            @Param("streetSegmentIds") List<Long> streetSegmentIds,
            @Param("incidentIds") Long[] incidentIds,
            @Param("lngs") Double[] lngs,
            @Param("lats") Double[] lats
    );


    @LogExecutionTimeSubTask
	@Query("SELECT p FROM PlanetOsmLine p WHERE p.id IN :ids")
	List<PlanetOsmLine> findByIds(@Param("ids") Collection<Long> ids);
}
