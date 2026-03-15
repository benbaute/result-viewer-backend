package com.simra.konsumgandalf.common.repositories;

import com.simra.konsumgandalf.common.models.entities.TrafficSignalCluster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TrafficSignalClusterRepository extends JpaRepository<TrafficSignalCluster, Long> {

	@Modifying
	@Transactional
	@Query(value = """
			WITH clusters AS (
			    SELECT id, ST_ClusterDBSCAN(geom25833, eps => 70, minpoints => 1) OVER () AS cluster_geom
			    FROM traffic_signal
			),
			clusterized AS (
			    SELECT array_agg(t.id) AS original_signal_ids
			    FROM traffic_signal t
			    JOIN clusters c ON t.id = c.id
			    GROUP BY c.cluster_geom
			)
			INSERT INTO traffic_signal_cluster (original_signal_ids)
			SELECT original_signal_ids
			FROM clusterized;
			""", nativeQuery = true)
	void setSignalIdsOnCluster();

	@Modifying
	@Transactional
	@Query(value = """
			    UPDATE traffic_signal_cluster c
			    SET geom = (
			        SELECT ST_Transform(ST_Buffer(ST_ConvexHull(ST_Collect(geom25833)), 25, 'quad_segs=2'), 4326)
			        FROM traffic_signal t
			        WHERE t.id = ANY(c.original_signal_ids)
			    );
			""", nativeQuery = true)
	void setClusterGeometry();

	@Modifying
	@Transactional
	@Query(value = """
			    UPDATE traffic_signal_cluster c
			    SET geom3857 = (
			        SELECT ST_Transform(geom, 3857)
			    )
			""", nativeQuery = true)
	void setClusterGeometry3857();

	@Modifying
	@Transactional
	@Query(value = """
			WITH intersection AS (
			    SELECT
			        c.id AS cluster_id,
			        l.osm_id AS line_id
			    FROM planet_osm_line l
			    JOIN traffic_signal_cluster c
			    ON ST_Intersects(l.way, c.geom3857)
			    WHERE l.highway is not null
			    AND l.highway NOT IN ('construction', 'elevator', 'motorway', 'motorway_link', 'platform', 'proposed')
			)
			INSERT INTO traffic_signal_cluster__planet_osm_line (traffic_signal_cluster_id, osm_id)
			SELECT cluster_id, line_id
			FROM intersection


			""", nativeQuery = true)
	void populateClusterLineRelations();

	@Modifying
	@Transactional
	@Query(value = """
			    UPDATE traffic_signal_cluster c
			    SET osm_lines_name = (
			        SELECT array_agg(name) FROM (
			        SELECT name, Count(line.name) AS c
			  		FROM planet_osm_line line
			  		JOIN traffic_signal_cluster__planet_osm_line cluster
			  		ON line.osm_id = cluster.osm_id
			  		WHERE cluster.traffic_signal_cluster_id = c.id
			  		AND name is not null
			  		AND surface is not null
					AND highway is not null
			        AND highway != 'footway'
			  		GROUP BY name
			  		ORDER BY c) as t
			    );

			""", nativeQuery = true)
	void setStreetNames();

	@Query(value = """
			SELECT l.id, c
			FROM TrafficSignalCluster c
			JOIN c.osmLines l
			WHERE l.id IN :osmLineIds
			""")
	List<Object[]> findByOsmLineIds(List<Long> osmLineIds);

	@Query(value = """
			SELECT c.*
			FROM traffic_signal_cluster c
			WHERE :trafficSignalId = ANY(c.original_signal_ids)
			""", nativeQuery = true)
	List<TrafficSignalCluster> findByTrafficSignalId(Long trafficSignalId);

	@Query(value = """
			SELECT *
			FROM traffic_signal_cluster
			WHERE :trafficSignalClusterId = id
			""", nativeQuery = true)
	List<TrafficSignalCluster> findByTrafficSignalClusterId(Long trafficSignalClusterId);

	@Query(value = """
			WITH
			-- Create the bounding box for the tile in Web Mercator (3857)
			bounds AS (
			  SELECT ST_TileEnvelope(:z, :x, :y) AS geom
			),
			mvtgeom AS (
			    SELECT ST_AsMVTGeom(t.geom3857, bounds.geom) AS geom, t.id
			    FROM traffic_signal_cluster t, bounds
			    WHERE t.geom3857 && bounds.geom
			)
			-- 3. Package into binary
			SELECT ST_AsMVT(mvtgeom.*, 'cluster-layer') FROM mvtgeom
			    """, nativeQuery = true)
	byte[] getClusterTile(@Param("z") int z, @Param("x") int x, @Param("y") int y);

}
