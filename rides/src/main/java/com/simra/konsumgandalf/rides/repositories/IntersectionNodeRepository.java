package com.simra.konsumgandalf.rides.repositories;


import com.simra.konsumgandalf.common.models.dtos.IntersectionNodeAggregate;
import com.simra.konsumgandalf.common.models.entities.IntersectionNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntersectionNodeRepository extends JpaRepository<IntersectionNode, Long> {
    List<IntersectionNode> findByRideId(Long rideId);

    @Query(
            name = "IntersectionNode.aggregateNodes",
            nativeQuery = true
    )
    List<IntersectionNodeAggregate> aggregateNodes(
            @Param("trafficSignalClusterId") Long trafficSignalClusterId,
            @Param("count") Long count,
            @Param("region") String region,
            @Param("streetNames") String streetNames
    );

    @Query(
            value = """
    SELECT *
    FROM intersection_node
    WHERE :trafficSignalClusterId = traffic_signal_cluster_id
        AND
            ((:startOsmId IS NULL AND start_osm_id IS NULL) OR
            (:startOsmId IS NOT NULL AND start_osm_id = :startOsmId))
        AND
            ((:endOsmId IS NULL AND end_osm_id IS NULL) OR
            (:endOsmId IS NOT NULL AND end_osm_id = :endOsmId))
    """,
            nativeQuery = true
    )
    List<IntersectionNode> findByClusterIdStartEndOsmId(Long trafficSignalClusterId, Long startOsmId, Long endOsmId);

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
                    FROM intersection_node__region nr
                    JOIN region r ON r.id = nr.region_id
                    WHERE nr.node_id = node.id
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
