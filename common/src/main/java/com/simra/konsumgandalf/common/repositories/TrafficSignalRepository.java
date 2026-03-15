package com.simra.konsumgandalf.common.repositories;

import com.simra.konsumgandalf.common.models.entities.TrafficSignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TrafficSignalRepository extends JpaRepository<TrafficSignal, Long> {
    @Modifying
    @Transactional
    @Query(value = """
    INSERT INTO traffic_signal (id, geom, geom25833)
    SELECT
        n.id,
        ST_SetSRID(ST_MakePoint(lon / 1e7, lat / 1e7), 4326),
        ST_Transform(ST_SetSRID(ST_MakePoint(lon / 1e7, lat / 1e7), 4326), 25833)
    FROM planet_osm_nodes n
    WHERE tags->>'highway' = 'traffic_signals'
    AND NOT EXISTS (
        SELECT 1
        FROM traffic_signal t
        WHERE t.id = n.id
    );
""", nativeQuery = true)
    void saveTrafficSignals();


    @Query(value = """
SELECT s.*
FROM traffic_signal s
JOIN traffic_signal_cluster c
ON s.id = ANY(c.original_signal_ids)
WHERE :trafficSignalClusterId = c.id
""", nativeQuery = true)
    List<TrafficSignal> findByTrafficSignalClusterId(Long trafficSignalClusterId);

    @Query(value = """
WITH
-- Create the bounding box for the tile in Web Mercator (3857)
bounds AS (
  SELECT ST_TileEnvelope(:z, :x, :y) AS geom
),
mvtgeom AS (
    -- Use available GIST index, TODO: use 3857 instead
    SELECT ST_AsMVTGeom(st_transform(t.geom, 3857), bounds.geom) AS geom, t.id
    FROM traffic_signal t, bounds
    WHERE t.geom && st_transform(bounds.geom, 4326)
)
-- 3. Package into binary
SELECT ST_AsMVT(mvtgeom.*, 'signal-layer') FROM mvtgeom -- layer name must match frontend
    """, nativeQuery = true)
    byte[] getSignalTile(@Param("z") int z, @Param("x") int x, @Param("y") int y);
}
