package com.simra.konsumgandalf.common.repositories;

import com.simra.konsumgandalf.common.models.entities.TrafficSignal;
import com.simra.konsumgandalf.common.models.entities.TrafficSignalCluster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TrafficSignalClusterRepository extends JpaRepository<TrafficSignalCluster, Long> {
    @Modifying
    @Transactional
    @Query(value = """

WITH projected AS (
    SELECT DISTINCT t.id, ST_Transform(geom, 25833) AS geom_utm,
           CASE
               WHEN l.highway = 'primary' OR l.highway = 'secondary' OR l.highway = 'cycleway' OR l.highway = 'path' THEN 80
               ELSE 40
            END AS influence_radius
    FROM traffic_signal t
    JOIN traffic_signal__planet_osm_line tl
    ON t.id = tl.traffic_signal_id
    JOIN planet_osm_line l
    ON tl.osm_line_id = l.osm_id
),
clusters AS (
    SELECT unnest(ST_ClusterWithin(geom_utm, influence_radius)) AS cluster_geom
    FROM projected
),
clusterized AS (
    SELECT
        array_agg(p.id) AS original_signal_ids
    FROM projected p
    JOIN clusters c
      ON ST_Within(p.geom_utm, c.cluster_geom)
    GROUP BY c.cluster_geom
)
INSERT INTO traffic_signal_cluster (original_signal_ids)
SELECT original_signal_ids
FROM clusterized;
""", nativeQuery = true)
    void generateClusters();

    @Modifying
    @Transactional
    @Query(value = """
    UPDATE traffic_signal_cluster c
    SET geom = (
        SELECT ST_Transform(ST_Buffer(ST_ConvexHull(ST_Collect(ST_Transform(t.geom, 25833))), 25, 'quad_segs=2'), 4326)
        FROM traffic_signal t
        WHERE t.id = ANY(c.original_signal_ids)
    )
""", nativeQuery = true)
    void updateClusterPolygons();


    @Modifying
    @Transactional
    @Query(value = """
WITH traffic_signal_cluster_projected AS (
    SELECT id, ST_Transform(geom, 25833) AS geom_utm
    FROM traffic_signal_cluster
),
planet_osm_line_projected AS (
    SELECT osm_id, ST_Transform(way, 25833) AS geom_utm
    FROM planet_osm_line
),
intersection AS (
SELECT
    c.id AS cluster_id,
    l.osm_id AS line_id
FROM planet_osm_line_projected l
JOIN traffic_signal_cluster_projected c
ON ST_Intersects(l.geom_utm, c.geom_utm))
INSERT INTO traffic_signal_cluster__planet_osm_line (traffic_signal_cluster_id, osm_id)
SELECT cluster_id, line_id
FROM intersection
""", nativeQuery = true)
    void populateClusterLineRelations();


    @Query(value = """
        SELECT name FROM (
        SELECT name, Count(line.name) AS c
  		FROM planet_osm_line line
  		JOIN traffic_signal_cluster__planet_osm_line cluster
  		ON line.osm_id = cluster.osm_id
  		WHERE cluster.traffic_signal_cluster_id = :trafficSignalClusterId
  		AND name is not null
  		AND surface is not null
		AND highway is not null
        AND highway != 'footway'
  		GROUP BY name
  		ORDER BY c) as t
""", nativeQuery = true)
    List<String> getNames(Long trafficSignalClusterId);


    @Query(value = """
SELECT c.*
FROM traffic_signal_cluster c
JOIN traffic_signal_cluster__planet_osm_line l
ON c.id = l.traffic_signal_cluster_id
WHERE l.osm_id = :osmLineId
""", nativeQuery = true)
    List<TrafficSignalCluster> findByOsmLineId(Long osmLineId);

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
}
