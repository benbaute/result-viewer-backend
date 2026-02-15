package com.simra.konsumgandalf.rides.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simra.konsumgandalf.common.logging.LogExecutionTime;
import com.simra.konsumgandalf.common.logging.LogExecutionTimeSubTask;
import com.simra.konsumgandalf.common.models.classes.MatchInformation;
import com.simra.konsumgandalf.common.models.classes.RideLocation;
import com.simra.konsumgandalf.common.models.entities.PlanetOsmLine;
import com.simra.konsumgandalf.common.models.entities.RideEntity;
import com.simra.konsumgandalf.common.models.entities.RideIncident;
import com.simra.konsumgandalf.common.models.enums.IncidentType;
import com.simra.konsumgandalf.common.models.maps.IxFunctionToParticipantTypeMap;
import com.simra.konsumgandalf.common.repositories.PlanetOsmLineRepository;
import com.simra.konsumgandalf.common.utils.services.CsvUtilService;
import com.simra.konsumgandalf.common.utils.services.FileReaderService;
import com.simra.konsumgandalf.common.utils.services.GeoService;
import com.simra.konsumgandalf.rides.repositories.RideEntityRepository;
import com.simra.konsumgandalf.valhalla.services.ValhallaTraceAttributesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.simra.konsumgandalf.common.constants.AppDates.*;

@Service
public class RideEntityService {

    @Autowired
    @Lazy // Crucial! Prevents "Circular Dependency" errors
    private RideEntityService self;

    private static Path dataPath;

	private static final ObjectMapper _objectMapper = new ObjectMapper();

	private static final Logger _logger = LoggerFactory.getLogger(RideEntityService.class);

	@Autowired
	private PlanetOsmLineRepository planetOsmLineRepository;

	@Autowired
	private RideEntityRepository rideEntityRepository;

	@Autowired
	private ValhallaTraceAttributesService valhallaTraceAttributesService;

	@Autowired
	private CsvUtilService csvUtilService;

	@Autowired
	private FileReaderService fileReaderService;

    @Autowired
    private RideService rideService;

    @Autowired
    private GeoService geoService;

    static final double MIN_DISTANCE_METERS = 3.0;
    static final double MAX_SPEED_METERS_PER_SECOND = 30.0; // Above 100 km/h for a bike

    RideEntityService(@Value("${SIMRA_RIDE_FILE_PATH:./}") String filePath) {
		dataPath = Paths.get(filePath);
	}

    @LogExecutionTime
	public int loadAllPreviousRides() {
		int counter = 0;

        for (String path : getNewRidePaths()) {
            generateNewRideEntity(path);
            _logger.info("[{}] Processed file: {}", ++counter, path);
        }

		_logger.info("Loaded {} new rides", counter);
        return counter;
	}

    private List<String> getNewRidePaths() {
        try {
            List<String> allPaths = Files.walk(dataPath, 8, FileVisitOption.FOLLOW_LINKS)
                    .filter(Files::isRegularFile)
                    .filter(FileReaderService::isEntityFile)
                    .map(Path::toString)
                    .toList();

            Set<String> existingPaths = new HashSet<>(rideEntityRepository.findExistingPaths(allPaths));
            return allPaths.stream().filter(path -> !existingPaths.contains(path)).toList();
        }
        catch (IOException e) {
            _logger.error("Error reading files from path: {}", dataPath, e);
            return Collections.emptyList();
        }
    }

	/**
	 * Add the CSV data to the ride entity.
	 * @param path - The path to the ride entity to enrich
	 * @return - The enriched ride entity
	 */
    @LogExecutionTimeSubTask
	public RideEntity enrichRideEntityWithCsv(String path) throws IllegalArgumentException {
		String content = fileReaderService.readFileFromPath(path);

		String[] filteredParts = Arrays.stream(content.split("=+"))
			.map(part -> Arrays.stream(part.split("\n"))
				.filter(line -> !line.contains("#"))
				.collect(Collectors.joining("\n"))
				.trim())
			.toArray(String[]::new);

		if (filteredParts.length < 2) {
            throw new IllegalArgumentException("File does not contain enough parts");
		}

		List<RideLocation> rideLocationList = csvUtilService.parseCsvToModel(filteredParts[1], RideLocation.class)
			.stream()
			.filter(this::validateRideLocation)
			.toList();

		if (rideLocationList.size() < 2) {
			throw new IllegalArgumentException("File does not contain enough ride locations");
		}

        RideEntity rideEntity = new RideEntity(path);
		rideEntity.setRideLocations(rideLocationList);
        rideEntity.setCleanLocations(getCleanCoordinates(rideLocationList));
        if (rideEntity.getCleanLocations().size() < 5) {
            throw new IllegalStateException("File does not contain enough clean ride locations, clean size: " +
                    rideEntity.getCleanLocations().size() + ", unfiltered size: " + rideLocationList.size());
        }

        rideEntity.setCoordinates(generateCoordinateString(rideLocationList));

		long[] rideTimestamps = rideLocationList.stream()
			.map(RideLocation::getTimeStamp)
			.collect(Collectors.teeing(Collectors.minBy(Long::compareTo), Collectors.maxBy(Long::compareTo),
					(min, max) -> {
						long minValue = (min.isEmpty() || min.get() < START_OF_RECORDING.getTime())
								? FALLBACK_DATE_MILLIS : min.get();
						long maxValue = (max.isEmpty() || max.get() < START_OF_RECORDING.getTime())
								? FALLBACK_DATE_MILLIS : max.get();

						return new long[] { minValue, maxValue };
					}));

		if (rideTimestamps[0] == FALLBACK_DATE_MILLIS || rideTimestamps[1] == FALLBACK_DATE_MILLIS) {
			throw new IllegalArgumentException("RideEntity uses fallback timestamp and will be discarded.");
		}

		rideEntity.setRideStart(new Date(rideTimestamps[0]));
		rideEntity.setRideEnd(new Date(rideTimestamps[1]));

		List<RideIncident> rideIncidentList = csvUtilService.parseCsvToModel(filteredParts[0], RideIncident.class);
		rideIncidentList = rideIncidentList.stream()
			.filter(incident -> incident.getIncidentType() != IncidentType.DUMMY_INCIDENT
					&& incident.getIncidentType() != IncidentType.NOTHING)
			.map(incident -> {
				IxFunctionToParticipantTypeMap.IxFunctionToParticipantType.forEach((key, value) -> {
					if (key.apply(incident) == 1) {
						incident.addParticipantsInvolved(value);
					}
				});

				Date dateOfIncident = getTimeStampFromRideIncident(incident, rideLocationList, rideTimestamps,
						rideEntity.getPath());
				incident.setTimeStamp(dateOfIncident);

				return incident;
			})
			.filter(this::validateRideIncident)
			.distinct()
			.toList();

		rideEntity.setRideIncidents(rideIncidentList);

		return rideEntity;
	}

    private ArrayList<MatchInformation> removeDuplicateTimeStamps(ArrayList<MatchInformation> coordinates) {
        ArrayList<MatchInformation> nonDuplicateCoordinates = new ArrayList<>();
        if (!coordinates.isEmpty()) {
            nonDuplicateCoordinates.add(coordinates.getFirst());
            for (MatchInformation current : coordinates) {
                long previousTime = nonDuplicateCoordinates.getLast().getTimestamp();
                long currentTime = current.getTimestamp();
                if (currentTime < previousTime) {
                    throw new RuntimeException("Timestamps not in order.");
                }
                if (currentTime != previousTime) {
                    nonDuplicateCoordinates.add(current);
                }
            }
        }
        return nonDuplicateCoordinates;
    }

    private ArrayList<MatchInformation> removeSpatialDuplicates(ArrayList<MatchInformation> coordinates) {
        ArrayList<MatchInformation> nonDuplicateCoordinates = new ArrayList<>();
        if (!coordinates.isEmpty()) {
            nonDuplicateCoordinates.add(coordinates.getFirst());
            for (MatchInformation current : coordinates) {
                MatchInformation previous = nonDuplicateCoordinates.getLast();
                if (geoService.distance(previous, current) > MIN_DISTANCE_METERS) {
                    nonDuplicateCoordinates.add(current);
                }
            }
        }
        return nonDuplicateCoordinates;
    }

    private ArrayList<MatchInformation> removeTeleportation(ArrayList<MatchInformation> coordinates) {
        ArrayList<MatchInformation> nonDuplicateCoordinates = new ArrayList<>();
        if (!coordinates.isEmpty()) {
            nonDuplicateCoordinates.add(coordinates.getFirst());
            for (int i = 1; i < coordinates.size(); i++) {
                MatchInformation previous = nonDuplicateCoordinates.getLast();
                MatchInformation current = coordinates.get(i);

                double dt = current.getTimestamp() - previous.getTimestamp();
                double speed = geoService.distance(previous, current) / dt; // m/s

                if (speed < MAX_SPEED_METERS_PER_SECOND) {
                    nonDuplicateCoordinates.add(current);
                }
            }
        }
        return nonDuplicateCoordinates;
    }

    private ArrayList<MatchInformation> getCleanCoordinates(List<RideLocation> rideLocationList) {
        ArrayList<MatchInformation> coordinates = new ArrayList<>(rideLocationList
                .stream()
                .map(location -> new MatchInformation(location.getLng(), location.getLat(), location.getTimeStamp() / 1000))
                .toList());

        return removeTeleportation(removeSpatialDuplicates(removeDuplicateTimeStamps(coordinates)));
    }

	/**
	 * Generate a new ride entity from a CSV file.
	 * @param path - The path to the CSV file
	 * @return - The generated ride entity
	 */
	public RideEntity generateNewRideEntity(String path) {
        try {
            RideEntity rideEntity = self.enrichRideEntityWithCsv(path);

            self.linkToPlanetOsmLine(rideEntity);
            rideService.processRideEntity(rideEntity);

            return rideEntityRepository.save(rideEntity);
        }
        catch (Exception e) {
            _logger.error("Error processing file: {}", path, e);
            // Create empty rideEntity, to avoid this file in later runs
            return rideEntityRepository.save(new RideEntity(path));
        }
	}

    /**
     * Links a ride entity and its incidents to the closest street segments in the planet
     * OSM line repository.
     * @param rideEntity - The csv enriched ride entity
     */
    @LogExecutionTimeSubTask
	public void linkToPlanetOsmLine(RideEntity rideEntity) {
		List<MatchInformation> coordinates = rideEntity.getCleanLocations();

		List<Long> streetSegmentIdsOfRoute = valhallaTraceAttributesService
			.calculateStreetSegmentIdsOfRoute(coordinates);
		if (streetSegmentIdsOfRoute.isEmpty()) {
			_logger.warn("Could not find any street segments for ride entity with path {}", rideEntity.getPath());
			return;
		}

		rideEntity.setPlanetOsmLines(planetOsmLineRepository.findByIds(streetSegmentIdsOfRoute));

        List<RideIncident> incidents = rideEntity.getRideIncidents();
        int numberOfIncidents = incidents.size();
        if (numberOfIncidents > 0) {
            Long[] incidentIds = new Long[numberOfIncidents];
            Double[] lngs = new Double[numberOfIncidents];
            Double[] lats = new Double[numberOfIncidents];

            for (int i = 0; i < numberOfIncidents; i++) {
                RideIncident inc = incidents.get(i);
                incidentIds[i] = (long) i;
                lngs[i] = inc.getLng();
                lats[i] = inc.getLat();
            }

            List<Long[]> matches = planetOsmLineRepository.findClosestStreetSegments(
                    streetSegmentIdsOfRoute, incidentIds, lngs, lats);

            for  (Long[] match : matches) {
                int incidentId = Math.toIntExact(match[0]);
                Long osmId = match[1];
                PlanetOsmLine ref = new PlanetOsmLine();
                ref.setId(osmId);
                incidents.get(incidentId).setPlanetOsmLine(ref);
            }
        }
	}

	/**
	 * Create a geometry from a list of ride locations.
	 * @param rideLocationList - A list of ride locations with coordinates
	 * @return - The entity that encapsulates the geometry
	 * @throws JsonProcessingException
	 */
	protected String generateCoordinateString(List<RideLocation> rideLocationList) {
		List<Map<String, Double>> coordinatesList = rideLocationList.stream().map(rideLocation -> {
			Map<String, Double> coordMap = new HashMap<>();
			coordMap.put("lng", rideLocation.getLng());
			coordMap.put("lat", rideLocation.getLat());
			return coordMap;
		}).collect(Collectors.toList());

		try {
			return _objectMapper.writeValueAsString(coordinatesList);
		}
		catch (JsonProcessingException e) {
			return "[]";
		}
	}

	/**
	 * This method tries to find the timestamp of a ride incident with multiple
	 * strategies.
	 * @param incident - The ride incident
	 * @param rideLocationList - The list of ride locations of the same ride as the
	 * incident
	 * @param rideTimestamps - The start and end timestamps of the ride
	 * @param ridePath - The path to the ride file
	 * @return - The timestamp of the ride incident
	 */
	protected Date getTimeStampFromRideIncident(RideIncident incident, List<RideLocation> rideLocationList,
			long[] rideTimestamps, String ridePath) {
		Date incidentDate = new Date(incident.getTs());
		if (isAfterStartOfRecording(incidentDate)) {
			return incidentDate;
		}

		Optional<Date> matchedDate = rideLocationList.stream()
			.filter(location -> location.getLng() == incident.getLng() && location.getLat() == incident.getLat())
			.findFirst()
			.map(location -> new Date(location.getTimeStamp()));

		if (matchedDate.isPresent() && isAfterStartOfRecording(matchedDate.get())) {
			return matchedDate.get();
		}

		if (rideTimestamps.length == 2) {
			incidentDate = new Date((rideTimestamps[0] + rideTimestamps[1]) / 2);
		}

		if (!isAfterStartOfRecording(incidentDate)) {
			try {
				incidentDate = fileReaderService.getFileLastModified(ridePath);
			}
			catch (Exception e) {
				_logger.error("Error getting last modified date of file", e);
			}
		}

		if (!isAfterStartOfRecording(incidentDate)) {
			incidentDate = FALLBACK_DATE;
		}

		return incidentDate;
	}

	protected boolean validateRideLocation(RideLocation rideLocation) {
		return rideLocation.getLat() != 0 && rideLocation.getLng() != 0 && rideLocation.getTimeStamp() != 0;
	}

	protected boolean validateRideIncident(RideIncident rideIncident) {
		return rideIncident.getLat() != 0 && rideIncident.getLng() != 0;
	}

	public Map<String, String[]> getRideGeometries(long id) {
		return rideEntityRepository.findRideGeometries(id);
	}

	private boolean isAfterStartOfRecording(Date date) {
		return (date != null) && date.after(START_OF_RECORDING);
	}
}
