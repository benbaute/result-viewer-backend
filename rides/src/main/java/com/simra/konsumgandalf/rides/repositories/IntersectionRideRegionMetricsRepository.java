package com.simra.konsumgandalf.rides.repositories;

import com.simra.konsumgandalf.common.models.entities.IntersectionRideRegionMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface IntersectionRideRegionMetricsRepository extends JpaRepository<IntersectionRideRegionMetrics, Long>,
		JpaSpecificationExecutor<IntersectionRideRegionMetrics> {

	@Modifying
	@Transactional
	@Query(value = """
			    REFRESH MATERIALIZED VIEW intersection_ride_region_metrics;
			""", nativeQuery = true)
	void updateIntersectionRideRegionMetrics();

	@Query(value = """
			SELECT *
			FROM intersection_ride_region_metrics
			WHERE region_id = :regionId
			AND week_day = :weekDay
			AND traffic_time = :trafficTime
			AND year = :year
			""", nativeQuery = true)
	List<IntersectionRideRegionMetrics> getIntersectionRideRegionMetrics(@Param("regionId") Long regionId,
			@Param("weekDay") String weekDay, @Param("trafficTime") String trafficTime, @Param("year") Integer year);

}
