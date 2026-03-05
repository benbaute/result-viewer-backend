package com.simra.konsumgandalf.rides.repositories;


import com.simra.konsumgandalf.common.models.entities.IntersectionEdge;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface IntersectionEdgeRepository extends JpaRepository<IntersectionEdge, Long> {
    List<IntersectionEdge> findByRideId(Long rideId);


    @Query(value = """
    SELECT DISTINCT e.ride.id
    FROM IntersectionEdge e
    WHERE e.line.id = :osmLineId
""")
    List<Long> findByOsmLineId(Long osmLineId);

    @Query(
            value = """
    SELECT edge
    FROM IntersectionEdge edge
    WHERE ((:prevOsmId IS NULL AND edge.prevLine IS NULL) OR
            (:prevOsmId IS NOT NULL AND edge.prevLine.id = :prevOsmId))
        AND
            ((:osmId IS NULL AND edge.line IS NULL) OR
            (:osmId IS NOT NULL AND edge.line.id = :osmId))
        AND
            ((:nextOsmId IS NULL AND edge.nextLine IS NULL) OR
            (:nextOsmId IS NOT NULL AND edge.nextLine.id = :nextOsmId))
    """
    )
    List<IntersectionEdge> findByPrevIdOsmIdNext(Long prevOsmId, Long osmId, Long nextOsmId);

    @Query(value = """
SELECT edge
FROM IntersectionEdge edge
WHERE ((:prevOsmId IS NULL AND edge.prevLine IS NULL) OR (:prevOsmId IS NOT NULL AND edge.prevLine.id = :prevOsmId))
AND ((:osmId IS NULL AND edge.line IS NULL) OR (:osmId IS NOT NULL AND edge.line.id = :osmId))
AND ((:nextOsmId IS NULL AND edge.nextLine IS NULL) OR (:nextOsmId IS NOT NULL AND edge.nextLine.id = :nextOsmId))
AND (:weekDay = 'ALL_WEEK' OR edge.weekDay = :weekDay)
AND (:trafficTime = 'ALL_DAY' OR edge.trafficTime = :trafficTime)
AND (:year = 2000 OR edge.year = :year)
""") List<IntersectionEdge> findByPrevIdOsmIdNext(
            @Param("prevOsmId") Long prevOsmId,
            @Param("osmId") Long osmId,
            @Param("nextOsmId") Long nextOsmId,
            @Param("trafficTime") TrafficTimes trafficTime,
            @Param("weekDay") WeekDays weekDay,
            @Param("year") Integer year);

    @Query(value = """
SELECT edge
FROM IntersectionEdge edge
WHERE ((:prevOsmId IS NULL AND edge.prevLine IS NULL) OR (:prevOsmId IS NOT NULL AND edge.prevLine.id = :prevOsmId))
AND ((:osmId IS NULL AND edge.line IS NULL) OR (:osmId IS NOT NULL AND edge.line.id = :osmId))
AND ((:nextOsmId IS NULL AND edge.nextLine IS NULL) OR (:nextOsmId IS NOT NULL AND edge.nextLine.id = :nextOsmId))
AND edge.startTime >= :startDate
AND edge.endTime <= :endDate
""") List<IntersectionEdge> findByPrevIdOsmIdNext(
            @Param("prevOsmId") Long prevOsmId,
            @Param("osmId") Long osmId,
            @Param("nextOsmId") Long nextOsmId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);

    @Query(
            value = """
    SELECT DISTINCT line.name
    FROM (
        SELECT
            COUNT(*) AS count,
            MIN(id) AS example_id
        FROM intersection_edge edge
        WHERE (
                :region IS NULL
                OR EXISTS (
                    SELECT 1
                    FROM intersection_base base
                    JOIN intersection__region ir ON base.id = ir.intersection_id
                    JOIN region r ON r.id = ir.region_id
                    WHERE base.id = edge.id
                    AND r.name = :region
                )
            )
        GROUP BY osm_id, prev_osm_id, next_osm_id
    ) aggregate
    JOIN intersection_edge example ON example.id = aggregate.example_id
    LEFT JOIN planet_osm_line line ON line.osm_id = example.osm_id
    WHERE (:count IS NULL OR aggregate.count >= :count)
    AND (:name IS NULL OR line.name ILIKE CONCAT('%', :name, '%'))
    """,
            nativeQuery = true
    )
    List<String> findAllStreetNames(Long count, String region, String name);
}
