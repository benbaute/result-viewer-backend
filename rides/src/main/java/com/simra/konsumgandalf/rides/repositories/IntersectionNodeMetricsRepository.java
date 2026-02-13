package com.simra.konsumgandalf.rides.repositories;

import com.simra.konsumgandalf.common.models.classes.IntersectionNodeMetricsIDKey;
import com.simra.konsumgandalf.common.models.entities.IntersectionNodeMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface IntersectionNodeMetricsRepository
        extends JpaRepository<IntersectionNodeMetrics, IntersectionNodeMetricsIDKey>,
        JpaSpecificationExecutor<IntersectionNodeMetrics> {

    @Modifying
    @Transactional
    @Query(value = """
    REFRESH MATERIALIZED VIEW intersection_node_metrics;
""", nativeQuery = true)
    void updateIntersectionNodeMetrics();
}
