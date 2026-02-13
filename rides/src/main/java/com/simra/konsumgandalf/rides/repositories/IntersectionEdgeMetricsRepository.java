package com.simra.konsumgandalf.rides.repositories;

import com.simra.konsumgandalf.common.models.classes.IntersectionEdgeMetricsIDKey;
import com.simra.konsumgandalf.common.models.entities.IntersectionEdgeMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface IntersectionEdgeMetricsRepository
        extends JpaRepository<IntersectionEdgeMetrics, IntersectionEdgeMetricsIDKey>,
        JpaSpecificationExecutor<IntersectionEdgeMetrics> {
}
