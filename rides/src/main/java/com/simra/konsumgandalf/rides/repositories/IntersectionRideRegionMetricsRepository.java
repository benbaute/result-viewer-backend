package com.simra.konsumgandalf.rides.repositories;

import com.simra.konsumgandalf.common.logging.LogExecutionTimeSubTask;
import com.simra.konsumgandalf.common.models.entities.IntersectionRideRegionMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface IntersectionRideRegionMetricsRepository extends JpaRepository<IntersectionRideRegionMetrics, Long>,
		JpaSpecificationExecutor<IntersectionRideRegionMetrics> {

	@LogExecutionTimeSubTask
	@Modifying
	@Transactional
	@Query(value = """
			    REFRESH MATERIALIZED VIEW intersection_ride_region_metrics;
			""", nativeQuery = true)
	void updateIntersectionRideRegionMetrics();

}
