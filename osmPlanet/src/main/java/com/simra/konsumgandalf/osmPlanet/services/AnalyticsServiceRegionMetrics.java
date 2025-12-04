package com.simra.konsumgandalf.osmPlanet.services;

import com.simra.konsumgandalf.common.logging.LogExecutionTime;
import com.simra.konsumgandalf.common.models.entities.Region;
import com.simra.konsumgandalf.common.models.entities.SafetyMetricsRegion;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.common.models.maps.DangerousScoreToColorMap;
import com.simra.konsumgandalf.osmPlanet.classes.dtos.RideEntityMetricsDTO;
import com.simra.konsumgandalf.osmPlanet.classes.dtos.RegionSafetyMetricsProjection;
import com.simra.konsumgandalf.osmPlanet.classes.dtos.RideEntityTotalDTO;
import com.simra.konsumgandalf.osmPlanet.classes.dtos.TimeFilters;
import com.simra.konsumgandalf.osmPlanet.repositories.OsmPolygonRepository;
import com.simra.konsumgandalf.osmPlanet.repositories.RegionRepository;
import com.simra.konsumgandalf.osmPlanet.repositories.SafetyMetricsPlanetOsmLineRepository;
import com.simra.konsumgandalf.osmPlanet.repositories.SafetyMetricsRegionRepository;
import com.simra.konsumgandalf.osmPlanet.repositories.SafetyMetricsSimraRegionRepository;
import com.simra.konsumgandalf.osmPlanet.utils.AnalyticsUtils;
import jakarta.transaction.Transactional;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.simra.konsumgandalf.osmPlanet.utils.TimeFilterUtils.getTimeFilters;
import static com.simra.konsumgandalf.common.utils.ScoreUtils.calculateDangerousScore;

/**
 * This service provides analytics for the OSM planet.
 */
@Transactional
@Service
public class AnalyticsServiceRegionMetrics {

	@Autowired
	private SafetyMetricsPlanetOsmLineRepository safetyMetricsLineRepository;

	@Autowired
	private SafetyMetricsRegionRepository safetyMetricsRegionRepository;

	@Autowired
	private SafetyMetricsSimraRegionRepository safetyMetricsSimraRegionRepository;

	@Autowired
	private RegionRepository regionRepository;

	@Autowired
	private OsmPolygonRepository osmPolygonRepository;

	private static final Logger _logger = LoggerFactory.getLogger(AnalyticsServiceRegionMetrics.class);

	private static final WKBReader _geometryReader = new WKBReader();

	public List<Region> tempRegions() {
		List<Region> regionNames = List.of(new Region("Berlin", -2145268L, 4), new Region("Berlin", -62422L, 4));
		List<Region> regions = regionNames.stream().map(p -> {
			Region region = new Region(p.getName(), p.getId(), Math.toIntExact(p.getAdminLevel()));
			byte[] byteWay = osmPolygonRepository.getWayByOsmId(region.getId());
			try {
				Geometry way = _geometryReader.read(byteWay);
				way.setSRID(4326);
				region.setWay(way);
			}
			catch (ParseException e) {
				throw new RuntimeException(e);
			}
			return region;
		}).toList();
		return regions;
	}

	@LogExecutionTime
	public void calculateSafetyMetricsRegion() {
		List<RegionSafetyMetricsProjection> statesSafetyMetrics = safetyMetricsLineRepository
			.getRegionSafetyMetricsOfAdminLevel(List.of("4", "6", "9"));

		_logger.info("Number of stateSafetyMetrics: {}", statesSafetyMetrics.size());
		Set<String> seenNames = new HashSet<>();
		_logger.info("Regions in DB: {}", regionRepository.findAll().stream().map(Region::getName).toList());
		List<Region> regions = statesSafetyMetrics.stream().filter(p -> seenNames.add(p.getName())).filter(p -> {
			Optional<Region> region = regionRepository.findByName(p.getName());
			return region.isEmpty() || region.get().getWay() == null;
		}).map(p -> {
			Region region = new Region(p.getName(), p.getOsmId(), Math.toIntExact(p.getAdminLevel()));
			byte[] byteWay = osmPolygonRepository.getWayByOsmId(region.getId());
			try {
				Geometry way = _geometryReader.read(byteWay);
				way.setSRID(4326);
				region.setWay(way);
			}
			catch (ParseException e) {
				throw new RuntimeException(e);
			}
			return region;
		}).toList();
		regions = regionRepository.saveAll(regions);
		_logger.info("All regions saved.");
		regions = regionRepository.findAll();

		List<RideEntityMetricsDTO> totalRidesAndLength = safetyMetricsRegionRepository.findNumberOfRidesAndLength();
		_logger.info("Total rides and length per region calculated.");

		ArrayList<SafetyMetricsRegion> safetyMetricsRegionList = new ArrayList<>();
		for (RegionSafetyMetricsProjection safetyMetricsProjection : statesSafetyMetrics) {
			Region region = regions.stream()
				.filter(r -> Objects.equals(r.getName(), safetyMetricsProjection.getName()))
				.findFirst()
				.orElseThrow();

			RideEntityTotalDTO totalRides = AnalyticsUtils.totalRideMetersPerRegion(totalRidesAndLength,
					region.getName(), safetyMetricsProjection.getTrafficTime(), safetyMetricsProjection.getWeekDay(),
					safetyMetricsProjection.getYear());

			if (totalRides.totalRides() == 0 || totalRides.totalDistance() == 0) {
				continue;
			}

			SafetyMetricsRegion safetyMetrics = new SafetyMetricsRegion(totalRides.totalDistance(),
					safetyMetricsProjection.getTrafficTime(), safetyMetricsProjection.getWeekDay(),
					safetyMetricsProjection.getYear(), Math.toIntExact(totalRides.totalRides()),
					Math.toIntExact(safetyMetricsProjection.getTotalIncidents()),
					Math.toIntExact(safetyMetricsProjection.getTotalScaryIncidents()),
					Math.toIntExact(safetyMetricsProjection.getTotalClosePasses()),
					Math.toIntExact(safetyMetricsProjection.getTotalPullInOuts()),
					Math.toIntExact(safetyMetricsProjection.getTotalNearLeftRightHooks()),
					Math.toIntExact(safetyMetricsProjection.getTotalHeadOnApproaches()),
					Math.toIntExact(safetyMetricsProjection.getTotalTailgating()),
					Math.toIntExact(safetyMetricsProjection.getTotalNearDoorings()),
					Math.toIntExact(safetyMetricsProjection.getTotalObstacleDodges()));

			float dangerousScore = calculateDangerousScore(Math.round(totalRides.totalDistance() / 1000),
					safetyMetrics.getNumberOfIncidents(), safetyMetrics.getNumberOfScaryIncidents());
			safetyMetrics.setDangerousScore(dangerousScore);

			String dangerousColor = DangerousScoreToColorMap.getColorForScore(dangerousScore);
			safetyMetrics.setDangerousColor(dangerousColor);

			safetyMetrics.setRegion(region);
			safetyMetrics.setName(region.getName());

			safetyMetricsRegionList.add(safetyMetrics);
			_logger.info("Safety metrics for region {} calculated - trafficTime: {}, weekDay: {}, year: {}",
					region.getName(), safetyMetricsProjection.getTrafficTime(), safetyMetricsProjection.getWeekDay(),
					safetyMetricsProjection.getYear());
		}
		safetyMetricsRegionRepository.saveAll(safetyMetricsRegionList);
		_logger.info("Safety metrics for regions calculated and saved.");
	}

	public boolean isEmpty() {
		return safetyMetricsRegionRepository.count() == 0;
	}

}
