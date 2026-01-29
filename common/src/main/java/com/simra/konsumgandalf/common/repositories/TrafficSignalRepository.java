package com.simra.konsumgandalf.common.repositories;

import com.simra.konsumgandalf.common.models.entities.TrafficSignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface TrafficSignalRepository extends JpaRepository<TrafficSignal, Long> {
    @Modifying
    @Transactional
    @Query(value = """
    UPDATE traffic_signal
    SET geom25833 = ST_Transform(geom, 25833);

    CREATE INDEX traffic_signal_geom25833_idx
    ON traffic_signal USING GIST (geom25833);
""", nativeQuery = true)
    void setGeom25833();

    @Query(value = """
			    SELECT id FROM TrafficSignal
			""")
    List<Long> getTrafficSignalIds();


    @Query(value = """
WITH traffic_signal_projected AS (
    SELECT id, ST_Transform(geom, 25833) AS geom_utm
    FROM traffic_signal
    WHERE id = :trafficSignalId
),
planet_osm_line_projected AS (
    SELECT osm_id, ST_Transform(way, 25833) AS geom_utm
    FROM planet_osm_line
    WHERE osm_id = :osmLineId
)
SELECT ST_Distance(l.geom_utm, s.geom_utm)
FROM planet_osm_line_projected l
JOIN traffic_signal__planet_osm_line sl
ON sl.osm_line_id = l.osm_id
JOIN traffic_signal_projected s
ON sl.traffic_signal_id = s.id
""", nativeQuery = true)
    double getDistanceOsmLineTrafficSignal(Long osmLineId, Long trafficSignalId);

    @Query(value = """
SELECT s.*
FROM traffic_signal s
JOIN traffic_signal__planet_osm_line l
ON s.id = l.traffic_signal_id
WHERE l.osm_line_id = :osmLineId
""", nativeQuery = true)
    List<TrafficSignal> findByOsmLineId(Long osmLineId);

    @Query(value = """
SELECT s.*
FROM traffic_signal s
JOIN traffic_signal_cluster c
ON s.id = ANY(c.original_signal_ids)
WHERE :trafficSignalClusterId = c.id
""", nativeQuery = true)
    List<TrafficSignal> findByTrafficSignalClusterId(Long trafficSignalClusterId);
}
