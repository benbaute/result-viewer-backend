package com.simra.konsumgandalf.rides.repositories;

import com.simra.konsumgandalf.common.models.entities.IntersectionNodeMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface IntersectionNodeMetricsRepository
		extends JpaRepository<IntersectionNodeMetrics, Long>, JpaSpecificationExecutor<IntersectionNodeMetrics> {

	@Modifying
	@Transactional
	@Query(value = """
			    REFRESH MATERIALIZED VIEW intersection_node_metrics;
			""", nativeQuery = true)
	void updateIntersectionNodeMetrics();

	@Query(value = """
			SELECT *
			FROM intersection_node_metrics
			WHERE number_of_rides >= :numberOfRides
			AND week_day = :weekDay
			AND traffic_time = :trafficTime
			AND year = :year
			""", nativeQuery = true)
	List<IntersectionNodeMetrics> getIntersectionNodeMetricsComplete(@Param("numberOfRides") Long numberOfRides,
			@Param("weekDay") String weekDay, @Param("trafficTime") String trafficTime, @Param("year") Integer year);

	@Query(value = """
			WITH
			bounds AS (
			  SELECT ST_TileEnvelope(:z, :x, :y) AS geom
			),
			mvtgeom AS (
			    SELECT
			        ST_AsMVTGeom(st_transform(i.geom, 3857), bounds.geom) AS geom,
			        i.traffic_signal_cluster_id as "trafficSignalClusterId",
			        i.start_valhalla_edge_id as "startValhallaEdgeId",
			        i.end_valhalla_edge_id as "endValhallaEdgeId",
			        i.week_day as "weekDay",
			        i.traffic_time as "trafficTime",
			        i.year,
			        i.example_id as id,
			        i.street_names as "streetNames",
			        i.start_osm_id as "startOsmId",
			        i.end_osm_id as "endOsmId",
			        i.number_of_rides as "numberOfRides",
			        i.median_length as "medianLength",
			        i.median_duration as "medianDuration",
			        i.median_speed as "medianSpeed",
			        i.max_waiting_time as "maxWaitingTime",
			        i.median_waiting_time as "medianWaitingTime"
			    FROM intersection_node_metrics i, bounds
			    WHERE i.number_of_rides >= :numberOfRides
			    AND i.week_day = :weekDay
			    AND i.traffic_time = :trafficTime
			    AND i.year = :year
			    AND i.geom && st_transform(bounds.geom, 4326)
			)
			SELECT ST_AsMVT(mvtgeom.*, 'node-metrics-layer') FROM mvtgeom -- layer name must match frontend
			    """, nativeQuery = true)
	byte[] getNodeMetricsTile(@Param("z") int z, @Param("x") int x, @Param("y") int y,
			@Param("numberOfRides") Long numberOfRides, @Param("weekDay") String weekDay,
			@Param("trafficTime") String trafficTime, @Param("year") int year);

	@Query(value = """
			WITH
			bounds AS (
			  SELECT ST_TileEnvelope(:z, :x, :y) AS geom
			),
			mvtgeom AS (
			    SELECT
			        ST_AsMVTGeom(st_transform(ST_StartPoint(i.geom), 3857), bounds.geom) AS geom,
			        i.traffic_signal_cluster_id as "trafficSignalClusterId",
			        i.start_valhalla_edge_id as "startValhallaEdgeId",
			        i.end_valhalla_edge_id as "endValhallaEdgeId",
			        i.week_day as "weekDay",
			        i.traffic_time as "trafficTime",
			        i.year,
			        i.example_id as id,
			        i.street_names as "streetNames",
			        i.start_osm_id as "startOsmId",
			        i.end_osm_id as "endOsmId",
			        i.number_of_rides as "numberOfRides",
			        i.median_length as "medianLength",
			        i.median_duration as "medianDuration",
			        i.median_speed as "medianSpeed",
			        i.max_waiting_time as "maxWaitingTime",
			        i.median_waiting_time as "medianWaitingTime"
			    FROM intersection_node_metrics i, bounds
			    WHERE i.number_of_rides >= :numberOfRides
			    AND i.week_day = :weekDay
			    AND i.traffic_time = :trafficTime
			    AND i.year = :year
			    AND i.geom && st_transform(bounds.geom, 4326)
			)
			SELECT ST_AsMVT(mvtgeom.*, 'node-metrics-start-layer') FROM mvtgeom -- layer name must match frontend
			    """, nativeQuery = true)
	byte[] getNodeMetricsStartTile(@Param("z") int z, @Param("x") int x, @Param("y") int y,
			@Param("numberOfRides") Long numberOfRides, @Param("weekDay") String weekDay,
			@Param("trafficTime") String trafficTime, @Param("year") int year);

}
