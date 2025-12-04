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
    SELECT id, ST_Transform(geom, 25833) AS geom_utm
    FROM traffic_signal
),
clusters AS (
    SELECT unnest(ST_ClusterWithin(geom_utm, 15)) AS cluster_geom
    FROM projected
),
clusterized AS (
    SELECT
        ST_Transform(ST_Centroid(ST_Collect(p.geom_utm)), 4326) AS geom,
        array_agg(p.id) AS original_signal_ids
    FROM projected p
    JOIN clusters c
      ON ST_Within(p.geom_utm, c.cluster_geom)
    GROUP BY c.cluster_geom
)
INSERT INTO traffic_signal_cluster (geom, original_signal_ids)
SELECT geom, original_signal_ids
FROM clusterized;
""", nativeQuery = true)
    void generateClusters();
}
