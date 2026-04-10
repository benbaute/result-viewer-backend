package com.simra.konsumgandalf.rides.repositories;

import com.simra.konsumgandalf.common.logging.LogExecutionTimeSubTask;
import com.simra.konsumgandalf.common.models.entities.IntersectionEdgeMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface IntersectionEdgeMetricsRepository
		extends JpaRepository<IntersectionEdgeMetrics, Long>, JpaSpecificationExecutor<IntersectionEdgeMetrics> {

	@LogExecutionTimeSubTask
	@Modifying
	@Transactional
	@Query(value = """
			    REFRESH MATERIALIZED VIEW intersection_edge_metrics;
			""", nativeQuery = true)
	void updateIntersectionEdgeMetrics();

	@Query(value = """
			WITH
			bounds AS (
			  SELECT ST_TileEnvelope(:z, :x, :y) AS geom
			),
			mvtgeom AS (
			    SELECT
			        ST_AsMVTGeom(st_transform(i.geom, 3857), bounds.geom) AS geom,
			        i.valhalla_edge_id as "valhallaEdgeId",
			        i.prev_valhalla_edge_id as "prevValhallaEdgeId",
			        i.next_valhalla_edge_id as "nextValhallaEdgeId",
			        i.week_day as "weekDay",
			        i.traffic_time as "trafficTime",
			        i.year,
			        i.example_id as id,
			        i.name,
			        i.osm_id as "osmId",
			        i.prev_osm_id as "prevOsmId",
			        i.next_osm_id as "nextOsmId",
			        i.number_of_rides as "numberOfRides",
			        i.avg_length as "avgLength",
			        i.avg_duration as "avgDuration",
			        i.avg_speed as "avgSpeed",
			        i.max_waiting_time as "maxWaitingTime",
			        i.avg_waiting as "avgWaitingTime",
					i.stop_rate as "stopRate",
					i.avg_waiting_when_stopped as "avgWaitingTimeWhenStopped"
			    FROM intersection_edge_metrics i, bounds
			    WHERE i.number_of_rides >= :numberOfRides
			    AND i.week_day = :weekDay
			    AND i.traffic_time = :trafficTime
			    AND i.year = :year
			    AND i.geom && st_transform(bounds.geom, 4326)
			)
			SELECT ST_AsMVT(mvtgeom.*, 'edge-metrics-layer') FROM mvtgeom -- layer name must match frontend
			    """, nativeQuery = true)
	byte[] getEdgeMetricsTile(@Param("z") int z, @Param("x") int x, @Param("y") int y,
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
			        i.valhalla_edge_id as "valhallaEdgeId",
			        i.prev_valhalla_edge_id as "prevValhallaEdgeId",
			        i.next_valhalla_edge_id as "nextValhallaEdgeId",
			        i.week_day as "weekDay",
			        i.traffic_time as "trafficTime",
			        i.year,
			        i.example_id as id,
			        i.name,
			        i.osm_id as "osmId",
			        i.prev_osm_id as "prevOsmId",
			        i.next_osm_id as "nextOsmId",
			        i.number_of_rides as "numberOfRides",
			        i.avg_length as "avgLength",
			        i.avg_duration as "avgDuration",
			        i.avg_speed as "avgSpeed",
			        i.max_waiting_time as "maxWaitingTime",
			        i.avg_waiting as "avgWaitingTime",
					i.stop_rate as "stopRate",
					i.avg_waiting_when_stopped as "avgWaitingTimeWhenStopped"
			    FROM intersection_edge_metrics i, bounds
			    WHERE i.number_of_rides >= :numberOfRides
			    AND i.week_day = :weekDay
			    AND i.traffic_time = :trafficTime
			    AND i.year = :year
			    AND i.geom && st_transform(bounds.geom, 4326)
			)
			SELECT ST_AsMVT(mvtgeom.*, 'edge-metrics-start-layer') FROM mvtgeom -- layer name must match frontend
			    """, nativeQuery = true)
	byte[] getEdgeMetricsStartTile(@Param("z") int z, @Param("x") int x, @Param("y") int y,
			@Param("numberOfRides") Long numberOfRides, @Param("weekDay") String weekDay,
			@Param("trafficTime") String trafficTime, @Param("year") int year);

}
