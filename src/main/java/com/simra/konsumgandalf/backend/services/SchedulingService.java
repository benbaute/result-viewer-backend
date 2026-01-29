package com.simra.konsumgandalf.backend.services;

import com.simra.konsumgandalf.common.constants.CronExpressions;
import com.simra.konsumgandalf.osmPlanet.services.AnalyticsServiceHighwayMetrics;
import com.simra.konsumgandalf.osmPlanet.services.AnalyticsServiceRegionMetrics;
import com.simra.konsumgandalf.osmPlanet.services.AnalyticsServiceSimraRegionMetrics;
import com.simra.konsumgandalf.osmPlanet.services.OsmHighwayService;
import com.simra.konsumgandalf.osmPlanet.services.RegionService;
import com.simra.konsumgandalf.profiles.services.AnalyticsProfileService;
import com.simra.konsumgandalf.profiles.services.ProfileService;
import com.simra.konsumgandalf.rides.services.RideEntityService;
import com.simra.konsumgandalf.rides.services.RideService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Profile("docker")
@Service
public class SchedulingService {

	@Autowired
	private RideService rideService;

    @Autowired
    private OsmService osmService;

	@Autowired
	private RideEntityService rideEntityService;

	@Autowired
	private AnalyticsServiceHighwayMetrics analyticsServiceHighwayMetrics;

	@Autowired
	private AnalyticsServiceRegionMetrics analyticsServiceRegionMetrics;

	@Autowired
	private AnalyticsServiceSimraRegionMetrics analyticsServiceSimraRegionMetrics;

	@Autowired
	private ProfileService profileService;

	@Autowired
	private AnalyticsProfileService analyticsProfileService;

	@Autowired
	private OsmHighwayService osmHighwayService;

	@Autowired
	private RegionService regionService;

	private static final Logger _logger = LoggerFactory.getLogger(SchedulingService.class);

	@Scheduled(cron = CronExpressions.EVERY_HOUR)
	public void readNewRidesAndCalculateSafetyMetrics() {
		rideEntityService.loadAllPreviousRidesBloomFilter();

		analyticsServiceHighwayMetrics.calculateSafetyMetricsHighway();
	}

	@Scheduled(cron = CronExpressions.EVERY_DAY)
	public void readNewRidesAndCalculateSafetyMetricsDaily() {
		this.analyseRegionBasedData();
	}

	@Scheduled(cron = CronExpressions.EVERY_WEEK)
	public void readNewRidesAndCalculateSafetyMetricsWeekly() {
		rideEntityService.loadAllPreviousRidesDatabase();

		analyticsServiceHighwayMetrics.calculateSafetyMetricsHighway();
		this.analyseRegionBasedData();
	}

	private void analyseRegionBasedData() {
		analyticsServiceRegionMetrics.calculateSafetyMetricsRegion();
		analyticsServiceSimraRegionMetrics.calculateSafetyMetricsSimraRegion();
	}

	@Scheduled(cron = CronExpressions.EVERY_DAY)
	public void readNewProfilesAndCalculateSafetyMetrics() {
		profileService.loadAllPrevProfiles();
		analyticsProfileService.calculateProfileSafetyMetrics();
	}

	@Scheduled(cron = CronExpressions.EVERY_DAY)
	public void exportJsons() {
		try {
			osmHighwayService.exportGridJson();
			regionService.exportPolygonJson();
		}
		catch (IOException e) {
			_logger.error("Error exporting JSON files: ", e);
		}
	}

	@Async
	@EventListener(ApplicationReadyEvent.class)
	public void init() {
		_logger.info("SchedulingService started");
		this.readNewRidesAndCalculateSafetyMetrics();
		//this.rideService.clearRides();
		//this.rideService.loadAllPreviousRides();

		if (analyticsServiceRegionMetrics.isEmpty()) {
			_logger.info("No region data found, calculating safety metrics for regions");
			analyticsServiceRegionMetrics.calculateSafetyMetricsRegion();
		}

		if (profileService.count() <= 3000L) {
			_logger.info("No profile data found, calculating safety metrics for profiles");
			readNewProfilesAndCalculateSafetyMetrics();
		}

        if (osmService.emptyTrafficSignals()) {
            try {
                osmService.saveTrafficSignals();
            } catch (IOException e) {
                _logger.error("Error while loading traffic signals", e);
            }
            if (!osmService.emptyTrafficSignals()) {
                osmService.setSpatialIndex();
            }
		}

		this.exportJsons();

		_logger.info("SchedulingService finished initialization");
	}

}
