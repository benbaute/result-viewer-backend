package com.simra.konsumgandalf.rides.repositories;

import com.simra.konsumgandalf.common.logging.LogExecutionTimeSubTask;
import com.simra.konsumgandalf.common.models.entities.IntersectionRideRegionMetrics;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.rides.classes.specifications.IntersectionRideRegionMetricsSpecifications;
import com.simra.konsumgandalf.rides.classes.specifications.TimeSpecifications;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface IntersectionRideRegionMetricsRepository
		extends RepositoryPropertiesMappable<IntersectionRideRegionMetrics, Long> {

	@LogExecutionTimeSubTask
	@Modifying
	@Transactional
	@Query(value = """
			      REFRESH MATERIALIZED VIEW intersection_ride_region_metrics_smallest_regions;
			      REFRESH MATERIALIZED VIEW intersection_ride_region_metrics;
			""", nativeQuery = true)
	void updateIntersectionRideRegionMetrics();

	default Specification<IntersectionRideRegionMetrics> createSpecification(Long regionId, WeekDays weekDay,
			TrafficTimes trafficTime, Integer year) {
		return Specification.where(IntersectionRideRegionMetricsSpecifications.hasRegionId(regionId))
			.and(TimeSpecifications.hasWeekDayBase(weekDay))
			.and(TimeSpecifications.hasTrafficTimeBase(trafficTime))
			.and(TimeSpecifications.hasYearBase(year));
	}

}
