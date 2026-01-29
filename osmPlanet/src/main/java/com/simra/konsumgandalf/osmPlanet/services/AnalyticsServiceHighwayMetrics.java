package com.simra.konsumgandalf.osmPlanet.services;

import com.google.common.collect.HashBiMap;
import com.simra.konsumgandalf.common.logging.LogExecutionTime;
import com.simra.konsumgandalf.common.models.entities.PlanetOsmLine;
import com.simra.konsumgandalf.common.models.entities.RideIncident;
import com.simra.konsumgandalf.common.models.entities.SafetyMetricsPlanetOsmLine;
import com.simra.konsumgandalf.common.models.enums.IncidentType;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.common.models.maps.DangerousScoreToColorMap;
import com.simra.konsumgandalf.osmPlanet.classes.dtos.FindNumberOfRidesWithinStreetSegmentInTimePeriodDTO;
import com.simra.konsumgandalf.osmPlanet.classes.keys.TrafficTimeWeekDayKey;
import com.simra.konsumgandalf.osmPlanet.repositories.OsmHighwayRepository;
import com.simra.konsumgandalf.osmPlanet.repositories.SafetyMetricsPlanetOsmLineRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import static com.simra.konsumgandalf.common.utils.ScoreUtils.calculateDangerousScore;
import static com.simra.konsumgandalf.osmPlanet.utils.TimeFilterUtils.getAllYears;

/**
 * This service provides analytics for the OSM planet.
 */
@Transactional
@Service
public class AnalyticsServiceHighwayMetrics {

	@Autowired
	private OsmHighwayRepository osmHighwayRepository;

	@Autowired
	private SafetyMetricsPlanetOsmLineRepository safetyMetricsLineRepository;

	@Autowired
	private OsmHighwayService osmHighwayService;

	private final int PAGE_SIZE;

	private static final Runtime runtime = Runtime.getRuntime();

	private static final Logger _logger = LoggerFactory.getLogger(AnalyticsServiceHighwayMetrics.class);

	public AnalyticsServiceHighwayMetrics(@Value("${ANALYTICS_PAGE_SIZE}") int pageSize) {
		PAGE_SIZE = pageSize;
	}

	@LogExecutionTime
	public void calculateSafetyMetricsHighway() {
		long startTime = System.nanoTime();
        int i = 0;

		_logger.info("Started to analyse planetOsmLine metrics");
		while (true) {
			List<PlanetOsmLine> fetchedStreets = osmHighwayRepository.findAllStreets(PageRequest.of(i, PAGE_SIZE));
            i++;
            if (fetchedStreets.isEmpty()) {
				break;
			}
            updateSafetyMetrics(fetchedStreets);
            _logger.info("Batch completed: {}", i);
		}
		_logger.info("All highway information updated in {} seconds.", (System.nanoTime() - startTime) / 1e9);
	}

	/**
	 * This method updates the safety metrics for a list of streets.
	 * @param streets - The list of streets to update the safety metrics for
	 */
    @Transactional
	void updateSafetyMetrics(List<PlanetOsmLine> streets) {
		ArrayList<SafetyMetricsPlanetOsmLine> safetyMetricsPlanetOsmLineList = new ArrayList<SafetyMetricsPlanetOsmLine>();
		for (PlanetOsmLine street : streets) {
			HashBiMap<TrafficTimeWeekDayKey, SafetyMetricsPlanetOsmLine> safetyMetrics = calculateSafetyMetrics(street);

			safetyMetrics.forEach((key, safetyMetric) -> {
				/*
				 * @TODO Anonymize the data later if (safetyMetric.getNumberOfRides() < 5)
				 * { return; }
				 */

				safetyMetric.setPlanetOsmLine(street);
				safetyMetric.setTrafficTime(key.getTrafficTime());
				safetyMetric.setWeekDay(key.getWeekDay());
				safetyMetric.setYear(key.getYear());
				safetyMetric.setOsmId(street.getId());
				safetyMetricsPlanetOsmLineList.add(safetyMetric);
			});
		}

		safetyMetricsLineRepository.saveAll(safetyMetricsPlanetOsmLineList);
		osmHighwayService.updateLastAnalysed(streets);
	}

	/**
	 * This method calculates the dangerousness score of a street. <br>
	 * Note this method has not implemented the logic of dynamic scariness factor
	 * calculation based on cities.
	 * @param street - The street to calculate the dangerousness score for
	 */
	HashBiMap<TrafficTimeWeekDayKey, SafetyMetricsPlanetOsmLine> calculateSafetyMetrics(PlanetOsmLine street) {
		HashBiMap<TrafficTimeWeekDayKey, SafetyMetricsPlanetOsmLine> trafficTimesSafetyMetricsHashBiMap = HashBiMap
			.create();

		for (FindNumberOfRidesWithinStreetSegmentInTimePeriodDTO ele : osmHighwayRepository
			.findNumberOfRidesWithinStreetSegmentInTimePeriod(street.getId())) {
			TrafficTimeWeekDayKey key = ele.getTrafficTimeWeekDayKey();

			SafetyMetricsPlanetOsmLine sm = new SafetyMetricsPlanetOsmLine();
			sm.setNumberOfRides(ele.getNumberOfRides());

			trafficTimesSafetyMetricsHashBiMap.put(key, sm);
		}

		for (RideIncident incident : street.getRideIncident()) {
			TrafficTimeWeekDayKey key = new TrafficTimeWeekDayKey(incident.getTrafficTime(), incident.getWeekDay(),
					incident.getYear());
			if (!trafficTimesSafetyMetricsHashBiMap.containsKey(key)) {
				_logger.warn("No safety metrics found for key: {}", key);
				continue;
			}
			SafetyMetricsPlanetOsmLine safetyMetricsPlanetOsmLine = trafficTimesSafetyMetricsHashBiMap.get(key);

			safetyMetricsPlanetOsmLine.setNumberOfIncidents(safetyMetricsPlanetOsmLine.getNumberOfIncidents() + 1);
			if (incident.isScary()) {
				safetyMetricsPlanetOsmLine
					.setNumberOfScaryIncidents(safetyMetricsPlanetOsmLine.getNumberOfScaryIncidents() + 1);
			}

			switch (incident.getIncidentType()) {
				case IncidentType.PULLING_IN_OUT -> safetyMetricsPlanetOsmLine
					.setNumberOfPullInOuts(safetyMetricsPlanetOsmLine.getNumberOfPullInOuts() + 1);
				case IncidentType.CLOSE_PASS -> safetyMetricsPlanetOsmLine
					.setNumberOfClosePasses(safetyMetricsPlanetOsmLine.getNumberOfClosePasses() + 1);
				case IncidentType.NEAR_LEFT_RIGHT_HOOK -> safetyMetricsPlanetOsmLine
					.setNumberOfNearLeftRightHooks(safetyMetricsPlanetOsmLine.getNumberOfNearLeftRightHooks() + 1);
				case IncidentType.HEAD_ON_APPROACH -> safetyMetricsPlanetOsmLine
					.setNumberOfHeadOnApproaches(safetyMetricsPlanetOsmLine.getNumberOfHeadOnApproaches() + 1);
				case IncidentType.TAILGATING -> safetyMetricsPlanetOsmLine
					.setNumberOfTailgating(safetyMetricsPlanetOsmLine.getNumberOfTailgating() + 1);
				case IncidentType.NEAR_DOORING -> safetyMetricsPlanetOsmLine
					.setNumberOfNearDoorings(safetyMetricsPlanetOsmLine.getNumberOfNearDoorings() + 1);
				case IncidentType.DODGING_OBSTACLE -> safetyMetricsPlanetOsmLine
					.setNumberOfObstacleDodges(safetyMetricsPlanetOsmLine.getNumberOfObstacleDodges() + 1);
			}

			trafficTimesSafetyMetricsHashBiMap.put(key, safetyMetricsPlanetOsmLine);
		}

		calculateAllYearValues(trafficTimesSafetyMetricsHashBiMap);

		trafficTimesSafetyMetricsHashBiMap.forEach((trafficTimeWeekDayKey, safetyMetrics) -> {
			safetyMetrics.setTrafficTime(trafficTimeWeekDayKey.getTrafficTime());
			safetyMetrics.setWeekDay(trafficTimeWeekDayKey.getWeekDay());
			safetyMetrics.setYear(trafficTimeWeekDayKey.getYear());

			float dangerousScore = calculateDangerousScore(safetyMetrics.getNumberOfRides(),
					safetyMetrics.getNumberOfIncidents(), safetyMetrics.getNumberOfScaryIncidents());
			safetyMetrics.setDangerousScore(dangerousScore);
			String dangerousColor = DangerousScoreToColorMap.getColorForScore(dangerousScore);
			safetyMetrics.setDangerousColor(dangerousColor);
		});

		return trafficTimesSafetyMetricsHashBiMap;
	}

	void calculateAllYearValues(HashBiMap<TrafficTimeWeekDayKey, SafetyMetricsPlanetOsmLine> metricsMap) {
		List<Integer> allYears = getAllYears(metricsMap);
		for (Integer year : allYears) {
			calculateAllWeekValues(metricsMap, year);
			calculateAllDayValues(metricsMap, year);
			calculateAllDayAllWeekValue(metricsMap, year);
		}

		HashBiMap<TrafficTimeWeekDayKey, SafetyMetricsPlanetOsmLine> tempAggregatedMap = HashBiMap.create();

		for (WeekDays wd : WeekDays.values()) {
			for (TrafficTimes tt : TrafficTimes.values()) {
				SafetyMetricsPlanetOsmLine aggregatedYearMetric = new SafetyMetricsPlanetOsmLine();
				boolean hasData = false;

				for (Integer year : allYears) {
					TrafficTimeWeekDayKey key = new TrafficTimeWeekDayKey(tt, wd, year);
					if (metricsMap.containsKey(key)) {
						aggregatedYearMetric.addUpSafetyMetric(metricsMap.get(key));
						hasData = true;
					}
				}

				if (hasData) {
					TrafficTimeWeekDayKey allYearKey = new TrafficTimeWeekDayKey(tt, wd, 2000);
					tempAggregatedMap.put(allYearKey, aggregatedYearMetric);
				}
			}
		}

		metricsMap.putAll(tempAggregatedMap);
	}

	void calculateAllDayValues(HashBiMap<TrafficTimeWeekDayKey, SafetyMetricsPlanetOsmLine> metricsMap, int year) {
		for (WeekDays wd : List.of(WeekDays.WEEK, WeekDays.WEEKEND)) {
			SafetyMetricsPlanetOsmLine aggregatedDayMetric = new SafetyMetricsPlanetOsmLine();
			for (TrafficTimes tt : List.of(TrafficTimes.MORNING_RUSH_HOUR, TrafficTimes.MID_DAY,
					TrafficTimes.EVENING_RUSH_HOUR, TrafficTimes.EVENING_NIGHT_MORNING)) {
				TrafficTimeWeekDayKey key = new TrafficTimeWeekDayKey(tt, wd, year);
				if (metricsMap.containsKey(key)) {
					aggregatedDayMetric.addUpSafetyMetric(metricsMap.get(key));
				}
			}
			TrafficTimeWeekDayKey allWeekKey = new TrafficTimeWeekDayKey(TrafficTimes.ALL_DAY, wd, year);

			if (aggregatedDayMetric.getNumberOfRides() == 0) {
				continue;
			}
			metricsMap.put(allWeekKey, aggregatedDayMetric);
		}
	}

	void calculateAllWeekValues(HashBiMap<TrafficTimeWeekDayKey, SafetyMetricsPlanetOsmLine> metricsMap, int year) {
		for (TrafficTimes tt : List.of(TrafficTimes.MORNING_RUSH_HOUR, TrafficTimes.MID_DAY,
				TrafficTimes.EVENING_RUSH_HOUR, TrafficTimes.EVENING_NIGHT_MORNING)) {
			SafetyMetricsPlanetOsmLine aggregatedDayMetric = new SafetyMetricsPlanetOsmLine();
			for (WeekDays wd : List.of(WeekDays.WEEK, WeekDays.WEEKEND)) {
				TrafficTimeWeekDayKey key = new TrafficTimeWeekDayKey(tt, wd, year);

				if (metricsMap.containsKey(key)) {
					aggregatedDayMetric.addUpSafetyMetric(metricsMap.get(key));
				}
			}
			TrafficTimeWeekDayKey allWeekKey = new TrafficTimeWeekDayKey(tt, WeekDays.ALL_WEEK, year);

			if (aggregatedDayMetric.getNumberOfRides() == 0) {
				continue;
			}
			metricsMap.put(allWeekKey, aggregatedDayMetric);
		}
	}

	void calculateAllDayAllWeekValue(HashBiMap<TrafficTimeWeekDayKey, SafetyMetricsPlanetOsmLine> metricsMap,
			int year) {
		SafetyMetricsPlanetOsmLine aggregatedDayMetric = new SafetyMetricsPlanetOsmLine();

		for (WeekDays wd : List.of(WeekDays.WEEK, WeekDays.WEEKEND)) {
			TrafficTimeWeekDayKey key = new TrafficTimeWeekDayKey(TrafficTimes.ALL_DAY, wd, year);

			if (metricsMap.containsKey(key)) {
				aggregatedDayMetric.addUpSafetyMetric(metricsMap.get(key));
			}
		}

		TrafficTimeWeekDayKey allDayAllWeekKey = new TrafficTimeWeekDayKey(TrafficTimes.ALL_DAY, WeekDays.ALL_WEEK,
				year);
		metricsMap.put(allDayAllWeekKey, aggregatedDayMetric);
	}

}
