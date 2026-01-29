package com.simra.konsumgandalf.rides.repositories;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.simra.konsumgandalf.common.models.dtos.IntersectionEdgeAggregate;
import com.simra.konsumgandalf.common.models.entities.IntersectionEdge;

@Repository
public interface IntersectionEdgeRepository extends JpaRepository<IntersectionEdge, Long> {
    List<IntersectionEdge> findByRideId(Long rideId);

    @Query(
            name = "IntersectionEdge.aggregateEdges",
            nativeQuery = true
    )
    List<IntersectionEdgeAggregate> aggregateEdges(Long count, String region, String name);

    @Query(value = """
    SELECT DISTINCT e.ride.id
    FROM IntersectionEdge e
    WHERE e.line.id = :osmLineId
""")
    List<Long> findByOsmLineId(Long osmLineId);

    @Query(
            value = """
    SELECT *
    FROM intersection_edge
    WHERE ((:prevOsmId IS NULL AND prev_osm_id IS NULL) OR
            (:prevOsmId IS NOT NULL AND prev_osm_id = :prevOsmId))
        AND
            ((:osmId IS NULL AND osm_id IS NULL) OR
            (:osmId IS NOT NULL AND osm_id = :osmId))
        AND
            ((:nextOsmId IS NULL AND next_osm_id IS NULL) OR
            (:nextOsmId IS NOT NULL AND next_osm_id = :nextOsmId))
    """,
            nativeQuery = true
    )
    List<IntersectionEdge> findByPrevIdOsmIdNext(Long prevOsmId, Long osmId, Long nextOsmId);

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
                    FROM intersection_edge__region er
                    JOIN region r ON r.name = er.region_id
                    WHERE er.edge_id = edge.id
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
