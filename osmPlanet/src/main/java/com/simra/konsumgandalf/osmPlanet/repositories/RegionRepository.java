package com.simra.konsumgandalf.osmPlanet.repositories;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.simra.konsumgandalf.common.models.dtos.RegionAggregate;
import com.simra.konsumgandalf.common.models.entities.Region;

public interface RegionRepository extends JpaRepository<Region, Long> {

	@EntityGraph(attributePaths = { "safetyMetricsRegions" })
	Optional<Region> findByName(String name);

	@Query("SELECT r.way FROM Region r WHERE r.name = :name")
	Optional<Geometry> findRegionWayByName(String name);

	Optional<Region> findBasicRegionByName(String name);

	@Query(value = "SELECT ST_AsBinary(ST_Union(g.way)) AS geom FROM (SELECT r.way FROM region r WHERE r.name IN (:names)) AS g",
			nativeQuery = true)
	byte[] unifyRegionWays(List<String> names);

	@Query("SELECT r.name FROM Region r WHERE r.name ILIKE :prefix% AND r.safetyMetricsRegions IS NOT EMPTY")
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
			        safety_metrics_region AS sm
			        ON region.name = sm.region_name
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


    @Query("""
        SELECT r FROM Region r
        WHERE function('ST_Contains', r.way, :point) = true
        ORDER BY r.adminLevel DESC
    """)
    List<Region> findContainingRegions(Point point);

/*
    @Query(value = """
WITH edges AS (
    SELECT
        edge.ride_id,
        intersection_edge__region.region_id,
        SUM(length)   AS edge_length,
        SUM(duration) AS edge_duration,
        SUM(waiting_time) AS edge_waiting_time,
        percentile_cont(0.5) WITHIN GROUP (ORDER BY waiting_time DESC) AS edge_median_waiting_time
    FROM intersection_edge edge
    JOIN intersection_edge__region ON edge.id = intersection_edge__region.edge_id
    GROUP BY edge.ride_id, intersection_edge__region.region_id
), nodes AS (
    SELECT
        node.ride_id,
        intersection_node__region.region_id,
        SUM(length)   AS node_length,
        SUM(duration) AS node_duration,
        SUM(waiting_time) AS node_waiting_time,
        percentile_cont(0.5) WITHIN GROUP (ORDER BY waiting_time DESC) AS node_median_waiting_time
    FROM intersection_node node
    JOIN intersection_node__region ON node.id = intersection_node__region.node_id
    GROUP BY node.ride_id, intersection_node__region.region_id
), combination AS (
    SELECT
        e.ride_id,
        e.region_id,
        
        -- raw totals
        e.edge_length,
        e.edge_duration,
        e.edge_waiting_time,
        e.edge_median_waiting_time,
        n.node_length,
        n.node_duration,
        n.node_waiting_time,
        n.node_median_waiting_time,
    
        -- combination
        n.node_duration + e.edge_duration AS ride_duration,
        n.node_length + e.edge_length AS ride_length,
        n.node_waiting_time / (n.node_duration + e.edge_duration) AS node_waiting_time_proportion,
        e.edge_waiting_time / (n.node_duration + e.edge_duration) AS edge_waiting_time_proportion
	FROM edges e
    JOIN nodes n
    ON e.ride_id = n.ride_id AND e.region_id = n.region_id
)
    SELECT
        region_id AS region_name,
        COUNT(region_id) AS count,
        SUM(ride_length)   AS ride_length,
        SUM(ride_duration) AS ride_duration,
        percentile_cont(0.5) WITHIN GROUP (ORDER BY node_median_waiting_time DESC) AS node_median_waiting_time,
        percentile_cont(0.5) WITHIN GROUP (ORDER BY edge_median_waiting_time DESC) AS edge_median_waiting_time,
        
        -- The following values have a problem with length
        -- The shorter the trip, the more bad is waiting time impact
        percentile_cont(0.5) WITHIN GROUP (ORDER BY node_waiting_time_proportion DESC) AS node_median_waiting_time_proportion,
        percentile_cont(0.5) WITHIN GROUP (ORDER BY edge_waiting_time_proportion DESC) AS edge_median_waiting_time_proportion
    FROM combination
    WHERE (:region IS NULL OR :region = region_id)
    GROUP BY region_id
		""", nativeQuery = true)
    List<Map<String, Object>> aggregateIntersectionDataPerRegion(String region);
    */

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
