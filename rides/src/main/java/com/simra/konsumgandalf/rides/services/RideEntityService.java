package com.simra.konsumgandalf.rides.services;

import com.simra.konsumgandalf.common.logging.LogExecutionTime;
import com.simra.konsumgandalf.common.models.entities.RideEntity;
import com.simra.konsumgandalf.rides.repositories.RideEntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
public class RideEntityService {

	@Autowired
	private RideEntityProcessorService rideEntityProcessorService;

	@Autowired
	private RideProcessorService rideProcessorService;

	private static Path dataPath;

	private static final Logger _logger = LoggerFactory.getLogger(RideEntityService.class);

	@Autowired
	private RideEntityRepository rideEntityRepository;

	RideEntityService(@Value("${SIMRA_RIDE_FILE_PATH:./}") String filePath) {
		dataPath = Paths.get(filePath);
	}

	@LogExecutionTime
	public int loadAllPreviousRides() {
		int counter = 0;

		for (String path : rideEntityProcessorService.getNewRidePaths(dataPath)) {
			generateNewRideEntity(path);
			_logger.info("[{}] Processed file: {}", ++counter, path);
		}

		_logger.info("Loaded {} new rides", counter);
		return counter;
	}

	/**
	 * Generate a new ride entity from a CSV file.
	 * @param path - The path to the CSV file
	 */
	private void generateNewRideEntity(String path) {
		try {
			RideEntity rideEntity = rideEntityProcessorService.enrichRideEntityWithCsv(path);

			rideEntityProcessorService.linkToPlanetOsmLine(rideEntity);
			rideProcessorService.processRideEntity(rideEntity);

			rideEntityProcessorService.saveRideEntity(rideEntity);
		}
		catch (Exception e) {
			_logger.error("Error processing file: {}", path, e);

			// Create empty rideEntity, to avoid this file in later runs
			rideEntityProcessorService.saveRideEntity(new RideEntity(path));
		}
	}

	public Map<String, String[]> getRideGeometries(long id) {
		return rideEntityRepository.findRideGeometries(id);
	}

}
