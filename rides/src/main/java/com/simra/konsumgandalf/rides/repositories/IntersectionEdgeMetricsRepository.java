package com.simra.konsumgandalf.rides.repositories;

import com.simra.konsumgandalf.common.models.classes.IntersectionEdgeMetricsIDKey;
import com.simra.konsumgandalf.common.models.entities.IntersectionEdgeMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface IntersectionEdgeMetricsRepository
        extends JpaRepository<IntersectionEdgeMetrics, IntersectionEdgeMetricsIDKey>,
        JpaSpecificationExecutor<IntersectionEdgeMetrics> {

    @Modifying
    @Transactional
    @Query(value = """
    REFRESH MATERIALIZED VIEW intersection_edge_metrics;
""", nativeQuery = true)
    void updateIntersectionEdgeMetrics();
}
