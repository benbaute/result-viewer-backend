package com.simra.konsumgandalf.rides.repositories;

import com.simra.konsumgandalf.common.logging.LogExecutionTimeSubTask;
import com.simra.konsumgandalf.common.models.entities.IntersectionRegionMetrics;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.rides.classes.specifications.IntersectionBaseMetricsSpecifications;
import com.simra.konsumgandalf.rides.classes.specifications.IntersectionRegionMetricsSpecifications;
import com.simra.konsumgandalf.rides.classes.specifications.TimeSpecifications;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface IntersectionRegionMetricsRepository
		extends RepositoryFeatureMappable<IntersectionRegionMetrics, Long> {

	@LogExecutionTimeSubTask
	@Modifying
	@Transactional
	@Query(value = """
			REFRESH MATERIALIZED VIEW intersection_region_metrics_smallest_regions;
			REFRESH MATERIALIZED VIEW intersection_region_ride_counts_metrics;
			REFRESH MATERIALIZED VIEW intersection_region_metrics;
			""", nativeQuery = true)
	void updateIntersectionRegionMetrics();

	@NonNull
	@EntityGraph(attributePaths = { "region" })
	Page<IntersectionRegionMetrics> findAll(Specification<IntersectionRegionMetrics> spec, @NonNull Pageable pageable);

	@Query(value = """
			SELECT m
			FROM IntersectionRegionMetrics m
			JOIN FETCH m.region r
			WHERE m.numberOfRides >= :numberOfRides
			AND r.adminLevel = :adminLevel
			AND m.weekDay = :weekDay
			AND m.trafficTime = :trafficTime
			AND m.year = :year
			""")
	List<IntersectionRegionMetrics> getIntersectionRegionMetricsComplete(@Param("numberOfRides") Long numberOfRides,
			@Param("adminLevel") Integer adminLevel, @Param("weekDay") WeekDays weekDay,
			@Param("trafficTime") TrafficTimes trafficTime, @Param("year") Integer year);

	default Specification<IntersectionRegionMetrics> createSpecification(Long regionId, String regionLTreePath,
			Integer adminLevel, Long numberOfRides, WeekDays weekDay, TrafficTimes trafficTime, Integer year,
			Pageable pageable) {

		return Specification.where(IntersectionRegionMetricsSpecifications.hasRegionId(regionId))
			.and(IntersectionRegionMetricsSpecifications.isInsideRegionPath(regionLTreePath))
			.and(IntersectionRegionMetricsSpecifications.hasAdminLevel(adminLevel))
			.and(IntersectionBaseMetricsSpecifications.hasMinCount(numberOfRides))
			.and(TimeSpecifications.hasWeekDayMetrics(weekDay))
			.and(TimeSpecifications.hasTrafficTimeMetrics(trafficTime))
			.and(TimeSpecifications.hasYearMetrics(year));
	}

}
