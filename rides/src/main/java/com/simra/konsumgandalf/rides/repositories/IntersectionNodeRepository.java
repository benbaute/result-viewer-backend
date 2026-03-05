package com.simra.konsumgandalf.rides.repositories;


import com.simra.konsumgandalf.common.models.entities.IntersectionNode;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface IntersectionNodeRepository extends JpaRepository<IntersectionNode, Long> {
    List<IntersectionNode> findByRideId(Long rideId);

    @Query(
            value = """
    SELECT node
    FROM IntersectionNode node
    WHERE :trafficSignalClusterId = node.trafficSignalCluster.id
        AND
            ((:startOsmId IS NULL AND node.startLine IS NULL) OR
            (:startOsmId IS NOT NULL AND node.startLine.id = :startOsmId))
        AND
            ((:endOsmId IS NULL AND node.endLine IS NULL) OR
            (:endOsmId IS NOT NULL AND node.endLine.id = :endOsmId))
    """
    )
    List<IntersectionNode> findByClusterIdStartEndOsmId(Long trafficSignalClusterId, Long startOsmId, Long endOsmId);

    @Query(value = """
SELECT node
FROM IntersectionNode node
WHERE :trafficSignalClusterId = node.trafficSignalCluster.id
AND ((:startOsmId IS NULL AND node.startLine IS NULL) OR (:startOsmId IS NOT NULL AND node.startLine.id = :startOsmId))
AND ((:endOsmId IS NULL AND node.endLine IS NULL) OR (:endOsmId IS NOT NULL AND node.endLine.id = :endOsmId))
AND (:weekDay = 'ALL_WEEK' OR node.weekDay = :weekDay)
AND (:trafficTime = 'ALL_DAY' OR node.trafficTime = :trafficTime)
AND (:year = 2000 OR node.year = :year)
""") List<IntersectionNode> findByClusterIdStartEndOsmId(
            @Param("trafficSignalClusterId") Long trafficSignalClusterId,
            @Param("startOsmId") Long startOsmId,
            @Param("endOsmId") Long endOsmId,
            @Param("trafficTime") TrafficTimes trafficTime,
            @Param("weekDay") WeekDays weekDay,
            @Param("year") Integer year);

    @Query(value = """
SELECT node
FROM IntersectionNode node
WHERE :trafficSignalClusterId = node.trafficSignalCluster.id
AND ((:startOsmId IS NULL AND node.startLine IS NULL) OR (:startOsmId IS NOT NULL AND node.startLine.id = :startOsmId))
AND ((:endOsmId IS NULL AND node.endLine IS NULL) OR (:endOsmId IS NOT NULL AND node.endLine.id = :endOsmId))
AND node.startTime >= :startDate
AND node.endTime <= :endDate
""") List<IntersectionNode> findByClusterIdStartEndOsmId(
            @Param("trafficSignalClusterId") Long trafficSignalClusterId,
            @Param("startOsmId") Long startOsmId,
            @Param("endOsmId") Long endOsmId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);

    @Query(value = """
    SELECT DISTINCT node.street_names
    FROM (
        SELECT MIN(node.id) AS example_id, Count(*) as count
        FROM intersection_node node
        WHERE street_names ILIKE CONCAT('%', :streetNames, '%')
        AND (:trafficSignalClusterId IS NULL OR :trafficSignalClusterId = traffic_signal_cluster_id)
        AND (
                :region IS NULL
                OR EXISTS (
                    SELECT 1
                    FROM intersection_base base
                    JOIN intersection__region ir ON base.id = ir.intersection_id
                    JOIN region r ON r.id = ir.region_id
                    WHERE base.id = node.id
                    AND r.name = :region
                )
            )
        GROUP BY start_osm_id, end_osm_id
    ) AS aggregate
    JOIN intersection_node node ON node.id = aggregate.example_id
    WHERE (:count IS NULL OR aggregate.count >= :count)
    """, nativeQuery = true)
    List<String> findAllIncludingString(Long trafficSignalClusterId, Long count, String region, String streetNames);
}
