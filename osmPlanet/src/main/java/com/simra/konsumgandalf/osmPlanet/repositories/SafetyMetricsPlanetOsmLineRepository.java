package com.simra.konsumgandalf.osmPlanet.repositories;

import com.simra.konsumgandalf.common.models.entities.SafetyMetricsPlanetOsmLine;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.osmPlanet.classes.dtos.RegionSafetyMetricsProjection;
import com.simra.konsumgandalf.osmPlanet.classes.dtos.SafetyMetricDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface SafetyMetricsPlanetOsmLineRepository
		extends JpaRepository<SafetyMetricsPlanetOsmLine, Long>, JpaSpecificationExecutor<SafetyMetricsPlanetOsmLine> {

	@Query(value = """
			SELECT s
			FROM SafetyMetricsPlanetOsmLine s
			WHERE s.planetOsmLine.id = :id
			AND s.trafficTime = :trafficTime
			AND s.weekDay = :weekDay
			AND s.year = :year
			""")
	Optional<SafetyMetricsPlanetOsmLine> findByStreetId(long id, TrafficTimes trafficTime, WeekDays weekDay, int year);

	@Query(value = """
			    SELECT
			        b.osm_id AS osmId,
			        b.name AS name,
			        b.admin_level AS adminLevel,
			        sm.traffic_time AS trafficTime,
			        sm.week_day AS weekDay,
			        sm.year AS year,
			        SUM(sm.number_of_incidents) AS totalIncidents,
			        SUM(sm.number_of_scary_incidents) AS totalScaryIncidents,
			        SUM(sm.number_of_close_passes) AS totalClosePasses,
			        SUM(sm.number_of_pull_in_outs) AS totalPullInOuts,
			        SUM(sm.number_of_near_left_right_hooks) AS totalNearLeftRightHooks,
			        SUM(sm.number_of_head_on_approaches) AS totalHeadOnApproaches,
			        SUM(sm.number_of_tailgating) AS totalTailgating,
			        SUM(sm.number_of_near_doorings) AS totalNearDoorings,
			        SUM(sm.number_of_obstacle_dodges) AS totalObstacleDodges
			    FROM safety_metrics_planet_osm_line sm
			    JOIN planet_osm_line pol
			      ON sm.planet_osm_line_osm_id = pol.osm_id
			      AND pol.last_modified IS NOT NULL
			      AND pol.last_analysed IS NOT NULL
			    JOIN planet_osm_polygon b
			      ON b.way && pol.way
			      AND ST_Contains(b.way, pol.way)
			    WHERE b.boundary = 'administrative'
			      AND b.admin_level IN (:adminLevel)
			    GROUP BY b.osm_id, b.name, b.admin_level, sm.traffic_time, sm.week_day, sm.year
			""", nativeQuery = true)
	List<RegionSafetyMetricsProjection> getRegionSafetyMetricsOfAdminLevel(List<String> adminLevel);

	@Query("""
			SELECT s.dangerousColor as dangerousColor, s.osmId as osmId
			FROM SafetyMetricsPlanetOsmLine s
			WHERE s.trafficTime = :trafficTime
			AND s.weekDay = :weekDay
			AND s.year = :year
			AND s.numberOfRides >= 5
			""")
	List<SafetyMetricDTO> getFilteredSafetyMetrics(TrafficTimes trafficTime, WeekDays weekDay, int year);

    @Modifying
    @Transactional
    @Query(value = """
    REFRESH MATERIALIZED VIEW safety_metrics__planet_osm_line;
""", nativeQuery = true)
    void updateSafetyMetricsPlanetOsmLine();
}
