package com.simra.konsumgandalf.osmPlanet.repositories;

import com.simra.konsumgandalf.common.models.entities.SafetyMetricsRegion;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.osmPlanet.classes.dtos.RideEntityMetricsDTO;
import com.simra.konsumgandalf.osmPlanet.classes.dtos.SafetyMetricRegionDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface SafetyMetricsRegionRepository
		extends JpaRepository<SafetyMetricsRegion, Long>, JpaSpecificationExecutor<SafetyMetricsRegion> {

	@Query("""
			SELECT s
			FROM SafetyMetricsRegion s
			WHERE s.region.name = :name
			""")
	List<SafetyMetricsRegion> findByName(String name);

	@Query("""
				SELECT re.name as name,
				r.trafficTime as trafficTime,
				r.weekDay as weekDay,
				r.year as year,
				COUNT(*) as totalRides,
				SUM(ST_Length_M(ST_Intersection(r.way, re.way))) as totalDistance
				FROM Region as re
				JOIN RideEntity as r ON st_intersects(re.way, r.way)
				GROUP BY re.name, r.trafficTime, r.weekDay, r.year
			""")
	List<RideEntityMetricsDTO> findNumberOfRidesAndLength();

	@Query("""
			SELECT s.dangerousColor as dangerousColor, s.name as name
			FROM SafetyMetricsRegion s
			WHERE s.trafficTime = :trafficTime
			AND s.weekDay = :weekDay
			AND s.year = :year
			AND s.numberOfRides >= 5
			""")
	List<SafetyMetricRegionDTO> getFilteredSafetyMetrics(TrafficTimes trafficTime, WeekDays weekDay, int year);

    @Modifying
    @Transactional
    @Query(value = """
    REFRESH MATERIALIZED VIEW safety_metrics__region;
""", nativeQuery = true)
    void updateSafetyMetricsRegion();
}
