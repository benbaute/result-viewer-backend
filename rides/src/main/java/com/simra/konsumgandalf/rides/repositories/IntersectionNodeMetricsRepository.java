package com.simra.konsumgandalf.rides.repositories;

import com.simra.konsumgandalf.common.models.entities.IntersectionNodeMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface IntersectionNodeMetricsRepository
        extends JpaRepository<IntersectionNodeMetrics, Long>,
        JpaSpecificationExecutor<IntersectionNodeMetrics> {

    @Modifying
    @Transactional
    @Query(value = """
    REFRESH MATERIALIZED VIEW intersection_node_metrics;
""", nativeQuery = true)
    void updateIntersectionNodeMetrics();

    @Query(value = """
SELECT *
FROM intersection_node_metrics
WHERE number_of_rides >= :numberOfRides
AND week_day = :weekDay
AND traffic_time = :trafficTime
AND year = :year
""", nativeQuery = true)
    List<IntersectionNodeMetrics> getIntersectionNodeMetricsComplete(
            @Param("numberOfRides") Long numberOfRides,
            @Param("weekDay") String weekDay,
            @Param("trafficTime") String trafficTime,
            @Param("year") Integer year
    );
}
