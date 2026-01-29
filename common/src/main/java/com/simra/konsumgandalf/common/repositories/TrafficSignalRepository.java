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
""", nativeQuery = true)
    void setGeom25833();

    @Modifying
    @Transactional
    @Query(value = """
    CREATE INDEX traffic_signal_geom25833_idx
    ON traffic_signal USING GIST (geom25833);
""", nativeQuery = true)
    void setSpatialIndex();

    @Query(value = """
			    SELECT id FROM TrafficSignal
			""")
    List<Long> getTrafficSignalIds();


    @Query(value = """
SELECT s.*
FROM traffic_signal s
JOIN traffic_signal_cluster c
ON s.id = ANY(c.original_signal_ids)
WHERE :trafficSignalClusterId = c.id
""", nativeQuery = true)
    List<TrafficSignal> findByTrafficSignalClusterId(Long trafficSignalClusterId);
}
