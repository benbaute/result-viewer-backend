package com.simra.konsumgandalf.backend.services;

import com.simra.konsumgandalf.common.constants.CronExpressions;
import com.simra.konsumgandalf.common.logging.LoggingAspect;
import com.simra.konsumgandalf.common.services.OsmService;
import com.simra.konsumgandalf.osmPlanet.services.OsmHighwayService;
import com.simra.konsumgandalf.osmPlanet.services.RegionService;
import com.simra.konsumgandalf.osmPlanet.services.SafetyMetricsService;
import com.simra.konsumgandalf.osmPlanet.services.SimraRegionService;
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
    private LoggingAspect  loggingAspect;

    @Autowired
    private OsmService osmService;

	@Autowired
	private RideEntityService rideEntityService;

    @Autowired
    private RideService rideService;

    @Autowired
    private SafetyMetricsService safetyMetricsService;

    @Autowired
	private OsmHighwayService osmHighwayService;

	@Autowired
	private RegionService regionService;

    @Autowired
    private SimraRegionService simraRegionService;

	private static final Logger _logger = LoggerFactory.getLogger(SchedulingService.class);


	@Scheduled(cron = CronExpressions.EVERY_DAY)
	public void readNewRidesAndCalculateSafetyMetrics() {
		int loadedRides = rideEntityService.loadAllPreviousRides();
        if (loadedRides > 0) {
            safetyMetricsService.updateSafetyMetrics();
            rideService.updateIntersectionMetrics();
        }
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
        // This requires that init PostGIS is finished:
        // The tables planet_osm_line, and planet_osm_nodes, and planet_osm_polygon must not be empty
		_logger.info("SchedulingService started");

        if (regionService.emptyRegions()) {
            _logger.info("No regions found, loading regions.");
            regionService.saveRegions();
            simraRegionService.createOrUpdateSimraRegions();
        }

        if (osmService.emptyTrafficSignals()) {
            _logger.info("No traffic signals found, loading traffic signals.");
            osmService.loadTrafficSignalData();
        }

        this.readNewRidesAndCalculateSafetyMetrics();
        this.exportJsons();

		_logger.info("SchedulingService finished initialization");
        loggingAspect.printAllStopWatches();
	}

}
