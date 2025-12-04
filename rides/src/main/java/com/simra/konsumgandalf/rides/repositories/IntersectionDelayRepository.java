package com.simra.konsumgandalf.rides.repositories;

import com.simra.konsumgandalf.common.models.entities.Edge;
import com.simra.konsumgandalf.common.models.entities.IntersectionDelay;
import com.simra.konsumgandalf.common.models.entities.MatchedPoint;
import com.simra.konsumgandalf.rides.records.EdgeSpeedStats;
import com.simra.konsumgandalf.rides.records.IntersectionDelayGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntersectionDelayRepository extends JpaRepository<IntersectionDelay, Long> {
    List<IntersectionDelay> findByRideId(Long rideId);

    @Query(
            value = """
            SELECT 
                g.start_osm_id AS startLineId,
                g.end_osm_id AS endLineId,
                g.count AS count,
                g.avg_length AS avgLength,
                g.avg_duration AS avgDuration,
                g.max_duration AS maxDuration,
                g.avg_speed AS avgSpeed,
                ST_AsGeoJSON(d.geom) AS exampleGeom
            FROM (
                SELECT 
                    start_osm_id,
                    end_osm_id,
                    COUNT(*) AS count,
                    AVG(length) AS avg_length,
                    AVG(duration) AS avg_duration,
                    MAX(duration) AS max_duration,
                    AVG(speed) AS avg_speed,
                    MIN(id) AS example_id
                FROM intersection_delay
                GROUP BY start_osm_id, end_osm_id
            ) g
            JOIN intersection_delay d
                ON d.id = g.example_id
            """,
            nativeQuery = true
    )
    List<IntersectionDelayGroup> aggregateDelays();
}
