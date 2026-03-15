package com.simra.konsumgandalf.osmPlanet.repositories;

import com.simra.konsumgandalf.common.logging.LogExecutionTimeSubTask;
import com.simra.konsumgandalf.common.models.entities.SafetyMetricsSimraRegion;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.osmPlanet.classes.dtos.RideEntityMetricsDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface SafetyMetricsSimraRegionRepository
		extends JpaRepository<SafetyMetricsSimraRegion, Long>, JpaSpecificationExecutor<SafetyMetricsSimraRegion> {

	Optional<SafetyMetricsSimraRegion> findDistinctByNameAndWeekDayAndTrafficTimeAndYear(String name, WeekDays weekDay,
			TrafficTimes trafficTime, int year);

	@Query("""
			SELECT s
			FROM SafetyMetricsSimraRegion s
			WHERE s.region.name = :name
			""")
	List<SafetyMetricsSimraRegion> findByName(String name);

	@Query("""
				SELECT re.name as name,
				r.trafficTime as trafficTime,
				r.weekDay as weekDay,
				r.year as year,
				COUNT(*) as totalRides,
				SUM(ST_Length_M(ST_Intersection(r.way, re.way))) as totalDistance
				FROM SimraRegion as re
				JOIN RideEntity as r ON st_intersects(re.way, r.way)
				WHERE re.way IS NOT NULL AND re.name != 'All'
				GROUP BY re.name, r.trafficTime, r.weekDay, r.year
			""")
	List<RideEntityMetricsDTO> findNumberOfRidesAndLengthNotAll();

	@Query("""
				SELECT 'All' as name,
				r.trafficTime as trafficTime,
				r.weekDay as weekDay,
				r.year as year,
				COUNT(*) as totalRides,
				SUM(ST_Length_M(r.way)) as totalDistance
				FROM RideEntity as r
				GROUP BY r.trafficTime, r.weekDay, r.year
			""")
	List<RideEntityMetricsDTO> findNumberOfRidesAndLengthAll();

	@LogExecutionTimeSubTask
	@Modifying
	@Transactional
	@Query(value = """
			    REFRESH MATERIALIZED VIEW safety_metrics__simra_region;
			""", nativeQuery = true)
	void updateSafetyMetricsSimraRegion();

}
