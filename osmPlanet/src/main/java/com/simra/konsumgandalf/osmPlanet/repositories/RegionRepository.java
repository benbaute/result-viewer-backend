package com.simra.konsumgandalf.osmPlanet.repositories;

import com.simra.konsumgandalf.common.logging.LogExecutionTimeSubTask;
import com.simra.konsumgandalf.common.models.dtos.RegionAggregate;
import com.simra.konsumgandalf.common.models.entities.Region;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long> {

    @Query("SELECT r FROM Region r WHERE r.name = :name")
	Optional<Region> findByName(String name);

	@Query("SELECT r.way FROM Region r WHERE r.name = :name")
	Optional<Geometry> findRegionWayByName(String name);

	Optional<Region> findBasicRegionByName(String name);



	@Query("SELECT r.name FROM Region r WHERE r.name ILIKE :prefix%")
	List<String> findAllNames(String prefix);

	@Query(value = """
			    WITH transformed_point AS (
			                 SELECT ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS pt
			    )
			    SELECT
			        region.name AS name,
			        ST_AsGeoJSON(ST_Simplify(region.way, :tolerance), 4326) as way,
			        sm.dangerous_color
			    FROM
			        region
			    JOIN
			        transformed_point
			        ON region.way && ST_Buffer(transformed_point.pt, :distanceFilter)
			    LEFT JOIN
			        safety_metrics__region AS sm
			        ON region.name = sm.name
			        AND (region.admin_level = :adminLevel OR region.admin_level = 9 AND :adminLevel = 6)
			        AND sm.traffic_time = :trafficTime
			        AND sm.week_day = :weekDay
			        AND sm.year = :year
			    WHERE sm.dangerous_color IS NOT NULL;
			""", nativeQuery = true)
	List<Map<String, Object>> findWays(int adminLevel, double longitude, double latitude, int distanceFilter,
			double tolerance, String trafficTime, String weekDay, int year);

    @Query(value = """
				SELECT
					r.name,
					r.admin_level,
					ST_AsGeoJSON(r.way) AS way
				FROM region r
			""", nativeQuery = true)
    List<Map<String, Object>> getPolygonRaw();


    @LogExecutionTimeSubTask
    @Query(value = """
WITH input_numbers AS (
        SELECT
            unnest(cast(:ids as bigint[])) as input_id,
            unnest(cast(:lngs as float[])) as input_lng,
            unnest(cast(:lats as float[])) as input_lat
),
input_points AS (
    SELECT
        input_id,
        ST_SetSRID(ST_MakePoint(input_lng, input_lat), 4326) AS input_point
    FROM input_numbers
)
SELECT
    ip.input_id,
    r.id
FROM input_points ip
CROSS JOIN LATERAL (
    SELECT region.id
    FROM region
    WHERE region.way && input_point AND ST_COVERS(region.way, input_point)
) r
    """, nativeQuery = true)
    List<Long[]> findContainingRegions(
            @Param("ids") Long[] ids,
            @Param("lngs") Double[] lngs,
            @Param("lats") Double[] lats
    );


    @Modifying
    @Transactional
    @Query(value = """
    INSERT INTO region (name, admin_level, id, way, geom3857)
    SELECT
        p.name,
        CAST(p.admin_level AS INT),
        p.osm_id,
        ST_Transform(p.way, 4326),
        p.way
    FROM planet_osm_polygon p
    JOIN (
        SELECT
            osm_id,
            MAX(way_area) AS area
        FROM planet_osm_polygon
        WHERE boundary = 'administrative'
          AND admin_level IN ('4', '6', '9')
        GROUP BY osm_id
    ) AS max_area ON max_area.osm_id = p.osm_id AND p.way_area = max_area.area
    AND NOT EXISTS (
        SELECT 1
        FROM region r
        WHERE r.id = p.osm_id
    );
""", nativeQuery = true)
    void saveRegions();


    @Query(
            name = "Region.aggregateRegions",
            nativeQuery = true
    )
    List<RegionAggregate> aggregateIntersectionDataPerRegion(
            @Param("region") String region
    );

    @Query(value="""
SELECT * FROM Region WHERE (:name IS NULL OR :name = name)
""", nativeQuery = true)
    List<Region> findRegionByName(String name);
}
