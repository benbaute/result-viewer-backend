package com.simra.konsumgandalf.rides.repositories;

import com.simra.konsumgandalf.common.models.classes.IntersectionEdgeMetricsIDKey;
import com.simra.konsumgandalf.common.models.classes.IntersectionNodeMetricsIDKey;
import com.simra.konsumgandalf.common.models.entities.IntersectionEdgeMetrics;
import com.simra.konsumgandalf.common.models.entities.IntersectionNodeMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface IntersectionNodeMetricsRepository
        extends JpaRepository<IntersectionNodeMetrics, IntersectionNodeMetricsIDKey>,
        JpaSpecificationExecutor<IntersectionNodeMetrics> {
}
