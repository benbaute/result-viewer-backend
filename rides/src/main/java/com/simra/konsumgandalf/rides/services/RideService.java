package com.simra.konsumgandalf.rides.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simra.konsumgandalf.common.logging.LogExecutionTime;
import com.simra.konsumgandalf.common.models.classes.MatchInformation;
import com.simra.konsumgandalf.common.models.classes.MatchInformationDate;
import com.simra.konsumgandalf.common.models.classes.RideLoc;
import com.simra.konsumgandalf.common.models.entities.*;
import com.simra.konsumgandalf.common.models.enums.IncidentType;
import com.simra.konsumgandalf.common.models.maps.IxFunctionToParticipantTypeMap;
import com.simra.konsumgandalf.common.repositories.PlanetOsmLineRepository;
import com.simra.konsumgandalf.common.utils.services.CsvUtilService;
import com.simra.konsumgandalf.common.utils.services.FileReaderService;
import com.simra.konsumgandalf.common.utils.services.GeoService;
import com.simra.konsumgandalf.common.utils.services.OsmService;
import com.simra.konsumgandalf.rides.records.EdgeSpeedStats;
import com.simra.konsumgandalf.rides.records.IntersectionDelayGroup;
import com.simra.konsumgandalf.rides.repositories.*;
import com.simra.konsumgandalf.valhalla.models.ValhallaTraceAttributesResponse;
import com.simra.konsumgandalf.valhalla.services.ValhallaMapMatchingService;
import com.simra.konsumgandalf.valhalla.services.ValhallaTraceAttributesService;
import jakarta.transaction.Transactional;
import org.locationtech.jts.geom.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.simra.konsumgandalf.common.constants.AppDates.*;

@Service
@Transactional
public class RideService {

	private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

	private static Path dataPath;

	private static final ObjectMapper _objectMapper = new ObjectMapper();

	private static final Logger _logger = LoggerFactory.getLogger(RideService.class);

	@Autowired
	private PlanetOsmLineRepository planetOsmLineRepository;

	@Autowired
	private RideRepository rideRepository;

	@Autowired
	private RidePointRepository ridePointRepository;

	@Autowired
	private MatchedPointRepository matchedPointRepository;

	@Autowired
	private EdgeRepository edgeRepository;

    @Autowired
    private IntersectionDelayRepository intersectionDelayRepository;

	@Autowired
	private GeoService geoService;

    @Autowired
    private OsmService osmService;

	@Autowired
	private CsvUtilService csvUtilService;

	@Autowired
	private FileReaderService fileReaderService;

	@Autowired
	private ValhallaMapMatchingService valhallaMapMatchingService;

	RideService(@Value("${SIMRA_RIDE_FILE_PATH:./}") String filePath) {
		dataPath = Paths.get(filePath);
	}

	public void clearRides() {
		rideRepository.truncateAllRideTables();
	}

	@LogExecutionTime
	public void loadAllPreviousRides() {
		List<CompletableFuture<Void>> futures = new ArrayList<>();
		AtomicInteger counter = new AtomicInteger(0);

		try {
			Files.walk(dataPath, 8, FileVisitOption.FOLLOW_LINKS)
				.filter(Files::isRegularFile)
				.filter(FileReaderService::isEntityFile)
				.map(Path::toString)
				.forEach(path -> {
					CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
						try {
							_logger
								.info("Processing file: " + path + " on thread: " + Thread.currentThread().getName());
							Ride ride = generateNewRide(path);
							if (ride == null) {
								throw new Error("Empty Ride");
							}
							rideRepository.save(ride);
							saveRidePoints(ride);
							_logger.info("Processed file: " + path);
							counter.incrementAndGet();
						}
						catch (Exception e) {
							_logger.error("Error processing file: " + path, e);
						}
					});
					futures.add(future);
				});
		}
		catch (IOException e) {
			_logger.error("Error reading files from path: " + dataPath, e);
		}

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		_logger.info("Loaded {} new rides", counter.get());
	}

	/**
	 * Add the CSV data to the ride.
	 * @param ride - The ride to enrich
	 * @return - The enriched ride
	 */
	protected Ride enrichRideWithCsv(Ride ride) {
		String content = fileReaderService.readFileFromPath(ride.getPath());

		String[] filteredParts = Arrays.stream(content.split("=+"))
			.map(part -> Arrays.stream(part.split("\n"))
				.filter(line -> !line.contains("#"))
				.collect(Collectors.joining("\n"))
				.trim())
			.toArray(String[]::new);

		if (filteredParts.length < 2) {
			_logger.error("File does not contain enough parts");
			return null;
		}

		List<RideLoc> rideLocationList = csvUtilService.parseCsvToModel(filteredParts[1], RideLoc.class)
			.stream()
			.filter(loc -> loc.getLat() != null && loc.getLng() != null)
			.toList();

		if (rideLocationList.size() < 2) {
			_logger.error("File does not contain enough ride locations");
			return null;
		}

		ride.setRideLocations(rideLocationList);

		long[] rideTimestamps = rideLocationList.stream()
			.map(RideLoc::getTimeStamp)
			.collect(Collectors.teeing(Collectors.minBy(Long::compareTo), Collectors.maxBy(Long::compareTo),
					(min, max) -> {
						long minValue = (min.isEmpty() || min.get() < START_OF_RECORDING.getTime())
								? FALLBACK_DATE_MILLIS : min.get();
						long maxValue = (max.isEmpty() || max.get() < START_OF_RECORDING.getTime())
								? FALLBACK_DATE_MILLIS : max.get();

						return new long[] { minValue, maxValue };
					}));

		if (rideTimestamps[0] == FALLBACK_DATE_MILLIS || rideTimestamps[1] == FALLBACK_DATE_MILLIS) {
			_logger.warn("RideEntity at path {} uses fallback timestamp and will be discarded.", ride.getPath());
			return null;
		}

		ride.setRideStart(new Date(rideTimestamps[0]));
		ride.setRideEnd(new Date(rideTimestamps[1]));

		List<RideIncident> rideIncidentList = csvUtilService.parseCsvToModel(filteredParts[0], RideIncident.class);

		return ride;
	}

	/**
	 * Generate a new ride entity from a CSV file.
	 * @param path - The path to the CSV file
	 * @return - The generated ride entity
	 */
	public Ride generateNewRide(String path) {
		Ride ride = new Ride(path);

		try {
			ride = enrichRideWithCsv(ride);
		}
		catch (IllegalArgumentException e) {
			_logger.error("Error enriching ride entity with CSV", e);
			throw new RuntimeException(e);
		}
		return ride;
	}

    private void fixDuplicates(ArrayList<MatchInformationDate> coordinates) {
        long laterTime = coordinates.getLast().getOriginalTimestamp().getTime();
        for (int i = coordinates.size() - 2; i >= 0; i--) {
            long currentTime = coordinates.get(i).getOriginalTimestamp().getTime();
            if (currentTime > laterTime) {
                throw new RuntimeException("Timestamps not in order.");
            }
            if (currentTime == laterTime) {
                coordinates.remove(i+1);
            }
            laterTime = currentTime;
        }

        long previousTimeStamp = coordinates.getFirst().getValhallaTimestamp();
        for (int i = 1; i <= coordinates.size() - 1; i++) {
            long currentTimeStamp = coordinates.get(i).getValhallaTimestamp();
            if (previousTimeStamp == currentTimeStamp) {
                currentTimeStamp += 1;
                coordinates.get(i).setValhallaTimestamp(currentTimeStamp);
            }
            previousTimeStamp = currentTimeStamp;
        }
    }

    private ArrayList<MatchInformationDate> getFilteredCoordinates(Ride ride) {
        ArrayList<MatchInformationDate> coordinates = new ArrayList<>(ride.getRideLocations()
                .stream()
                .map(location -> new MatchInformationDate(location.getLng(), location.getLat(),
                        new Date(location.getTimeStamp()), location.getTimeStamp() / 1000))
                .toList());

        fixDuplicates(coordinates);

        List<Double> speeds = geoService.calculateSpeed(coordinates);
        List<Double> medianSpeeds = geoService.centeredMovingMedian(speeds, 3);
        for (int i = medianSpeeds.size() - 1; i >= 0; i--) {
            if (medianSpeeds.get(i) < 1) {
                coordinates.remove(i);
            }
        }

        if (coordinates.size() < 2) {
            throw new RuntimeException("Ride does not contain enough locations, after filtering low speeds.");
        }
        return coordinates;
    }

	public void saveRidePoints(Ride ride) {
		ArrayList<MatchInformationDate> coordinates = getFilteredCoordinates(ride);
		Map<String, Object> traceAttributes = valhallaMapMatchingService.getTraceAttributes(coordinates);

		if (traceAttributes == null || traceAttributes.isEmpty() || traceAttributes.get("edges") == null
				|| traceAttributes.get("matched_points") == null) {
			throw new RuntimeException("Valhalla Error, Missing trace attributes for Ride");
		}

		ArrayList<HashMap<String, Object>> matched_points = (ArrayList<HashMap<String, Object>>) traceAttributes
			.get("matched_points");
		if (matched_points.size() != coordinates.size()) {
			throw new RuntimeException("Mismatch between matched Points and Ride locations");
		}
		for (int i = coordinates.size()-1; i >= 0; i--) {
			HashMap<String, Object> matchedPoint = matched_points.get(i);
			if (matchedPoint.get("error") != null) {
                // coordinates.remove(i);
                matched_points.remove(i);
			}
			else {
                matchedPoint.put("timestamp", coordinates.get(i).getOriginalTimestamp());
                Coordinate coordinate = new Coordinate((double) matchedPoint.get("lon"),
                        (double) matchedPoint.get("lat"));
                matchedPoint.put("coordinate", coordinate);
			}
		}

		ArrayList<HashMap<String, Object>> edges = (ArrayList<HashMap<String, Object>>) traceAttributes.get("edges");
        for (HashMap<String, Object> edge : edges) {
            if (edge.get("way_id") != null) {
                Integer wayId = (Integer) edge.get("way_id");
                Optional<PlanetOsmLine> pLine = planetOsmLineRepository.findById(Long.valueOf(wayId));
                if (pLine.isPresent()) {
                    edge.put("osm_line", pLine.get());
                    List<TrafficSignal> trafficSignals = osmService.findTrafficSignalsByOsmLineId(Long.valueOf(wayId));
                    if (!trafficSignals.isEmpty()) {
                        // double distance = osmService.getDistanceOsmLineTrafficSignal(Long.valueOf(wayId), trafficSignal.getId());
                        // TODO: merge signals
                        // TODO: kreuzungspassierzeiten berechnen auf Basis osm Ids,
                        // Wenn Distanz Signal Matched Point kleiner als Threshold, Kreuzungsanfang
                        // Krezungsende, wenn osm line von Länge größer als 100 meter
                        // Distanz kleiner für kleinere Straßen? Wenn residential street kleiner als 50 meter?
                        edge.put("traffic_signals", trafficSignals);
                    }
                }
                else {
                    throw new RuntimeException("Could not find line with id " + wayId);
                }
            }
        }

		for (MatchInformationDate loc : coordinates) {
			RidePoint p = new RidePoint();
			p.setRide(ride);
			p.setTimestamp(loc.getOriginalTimestamp());
			Coordinate coord = new Coordinate(loc.getLng(), loc.getLat());
			p.setGeom(geometryFactory.createPoint(coord));
			ridePointRepository.save(p);
		}

        List<List<HashMap<String, Object>>> sortedPoints = new ArrayList<>();
        List<HashMap<String, Object>> currentPoints = new ArrayList<>();
        Integer currentWayId = null;
        for (HashMap<String, Object> matchedPoint : matched_points) {
            if (matchedPoint.get("edge_index") != null) {
                int edgeIndex = (int) matchedPoint.get("edge_index");
                HashMap<String, Object> edge = edges.get(edgeIndex);
                Integer wayId = (Integer) edge.get("way_id");
                matchedPoint.put("way_id", wayId);
                matchedPoint.put("osm_line", edge.get("osm_line"));
                if (!wayId.equals(currentWayId)) {
                    currentWayId = wayId;
                    if (!currentPoints.isEmpty()) {
                        sortedPoints.add(currentPoints);
                        currentPoints = new ArrayList<>();
                    }
                }
                currentPoints.add(matchedPoint);
            } else {
                if (currentWayId != null) {
                    currentWayId = null;
                    if (!currentPoints.isEmpty()) {
                        sortedPoints.add(currentPoints);
                        currentPoints = new ArrayList<>();
                    }
                }
                currentPoints.add(matchedPoint);
            }
        }
        if (!currentPoints.isEmpty()) {
            sortedPoints.add(currentPoints);
            currentPoints = new ArrayList<>();
        }

        for (int i = sortedPoints.size()-1; i >= 0; i--) {
            Integer wayId = (Integer) sortedPoints.get(i).getFirst().get("way_id");
            if (wayId == null) {
                if (i == sortedPoints.size()-1 || i == 0) {
                    sortedPoints.remove(i);
                } else {
                    Integer prevWayId = (Integer) sortedPoints.get(i-1).getFirst().get("way_id");
                    Integer nextWayId = (Integer) sortedPoints.get(i+1).getFirst().get("way_id");
                    if (prevWayId == null) {
                        sortedPoints.get(i-1).addAll(sortedPoints.get(i));
                        sortedPoints.remove(i);
                    } else if (nextWayId == null) {
                        throw new RuntimeException("Unexpected  null error.");
                    } else if (prevWayId.equals(nextWayId)) {
                        sortedPoints.get(i-1).addAll(sortedPoints.get(i+1));
                        sortedPoints.remove(i+1);
                        sortedPoints.remove(i);
                    }
                }
            } else {
                if (i > 0) {
                    Integer prevWayId = (Integer) sortedPoints.get(i-1).getFirst().get("way_id");
                    if (wayId.equals(prevWayId)) {
                        sortedPoints.get(i-1).addAll(sortedPoints.get(i));
                        sortedPoints.remove(i);
                    }
                }
            }
        }

        List<HashMap<String, Object>> unsortedPoints = new ArrayList<>();
        for (List<HashMap<String, Object>> sortedPoint : sortedPoints) {
            unsortedPoints.addAll(sortedPoint);
        }

        boolean inCrossing = false;
        String highway = "default";
        List<TrafficSignal> trafficSignals = new ArrayList<>();
        for (int i = 0; i < unsortedPoints.size(); i++) {
            HashMap<String, Object> matchedPoint = unsortedPoints.get(i);
            Coordinate coordinate = (Coordinate) matchedPoint.get("coordinate");
            Double smallestDistance = getSmallestDistanceToTrafficSignals(trafficSignals, coordinate);

            if (matchedPoint.get("osm_line") != null) {
                int edgeIndex = (int) matchedPoint.get("edge_index");
                HashMap<String, Object> edge = edges.get(edgeIndex);
                PlanetOsmLine osmLine = (PlanetOsmLine) edge.get("osm_line");
                highway = osmLine.getHighway();
                if (edge.get("traffic_signals") != null) {
                    List<TrafficSignal> currentTrafficSignals = (List<TrafficSignal>) edge.get("traffic_signals");
                    Double smallestDistanceToCurrentTrafficSignals = getSmallestDistanceToTrafficSignals(currentTrafficSignals, coordinate);
                    if (smallestDistanceToCurrentTrafficSignals != null && (smallestDistance == null
                            || smallestDistanceToCurrentTrafficSignals <= smallestDistance)) {
                        trafficSignals = currentTrafficSignals;
                        smallestDistance = smallestDistanceToCurrentTrafficSignals;
                    }
                    matchedPoint.put("traffic_signals", currentTrafficSignals);
                }
            }
            if (inCrossing) {
                if (smallestDistance != null && smallestDistance > distanceSignalOnHighway(highway)) {
                    inCrossing = false;
                } else {
                    matchedPoint.put("intersection", true);
                }
            } else {
                if (smallestDistance != null && smallestDistance < distanceSignalOnHighway(highway)) {
                    inCrossing = true;
                    matchedPoint.put("intersection", true);
                }
            }
            // TODO: What about points before intersection???
        }

        List<List<HashMap<String, Object>>> allIntersections = new ArrayList<>();
        List<HashMap<String, Object>> currentIntersections = new ArrayList<>();
        for (int i = 0; i < sortedPoints.size(); i++) {
            for (int j = 0; j < sortedPoints.get(i).size(); j++) {
                HashMap<String, Object> point = sortedPoints.get(i).get(j);
                if (point.get("intersection") != null) {
                    currentIntersections.add(point);
                } else {
                    if (!currentIntersections.isEmpty()) {
                        allIntersections.add(currentIntersections);
                        currentIntersections = new ArrayList<>();
                    }
                }
            }
        }
        if (!sortedPoints.isEmpty() && !sortedPoints.getFirst().isEmpty()
                && sortedPoints.getFirst().getFirst().get("intersection") != null
                && sortedPoints.getFirst().getFirst() == allIntersections.getFirst().getFirst()) {
            // Removes, the first intersection, if the ride starts with an intersection
            // If an intersection is not finished at the end of the ride, it has not to be removed, as only
            // complete intersections are added.
            allIntersections.removeFirst();
        }

        for (int i = allIntersections.size() - 1; i >= 1; i--) {
            // merge intersections based on distance
            List<HashMap<String, Object>> currentIntersection = allIntersections.get(i);
            List<HashMap<String, Object>> previousIntersection = allIntersections.get(i-1);
            HashMap<String, Object> firstPoint = currentIntersection.getFirst();
            HashMap<String, Object> lastPoint = previousIntersection.getLast();
            Coordinate coordinateFirst = (Coordinate) firstPoint.get("coordinate");
            Coordinate coordinateLast = (Coordinate) lastPoint.get("coordinate");

            String highwayFirst = "default";
            String highwayLast = "default";
            if (firstPoint.get("osm_line") != null) {
                PlanetOsmLine osmLine = (PlanetOsmLine) firstPoint.get("osm_line");
                highwayFirst = osmLine.getHighway();
            }
            if (lastPoint.get("osm_line") != null) {
                PlanetOsmLine osmLine = (PlanetOsmLine) lastPoint.get("osm_line");
                highwayLast = osmLine.getHighway();
            }
            double distance = geoService.distance(coordinateFirst, coordinateLast);
            if (distance < distanceMerge(highwayFirst, highwayLast)) {
                System.out.println("Merged");
                previousIntersection.addAll(currentIntersection);
                allIntersections.remove(i);
            }
        }

        for (List<HashMap<String, Object>> intersection : allIntersections) {
            if (intersection.size() < 2) {
                continue;
            }
            IntersectionDelay intersectionDelay = new IntersectionDelay();
            Date endTime = (Date) intersection.getLast().get("timestamp");
            Date startTime = (Date) intersection.getFirst().get("timestamp");
            intersectionDelay.setEndTime(endTime);
            intersectionDelay.setStartTime(startTime);
            intersectionDelay.setRide(ride);
            intersectionDelay.setEndLine((PlanetOsmLine) intersection.getLast().get("osm_line"));
            intersectionDelay.setStartLine((PlanetOsmLine) intersection.getFirst().get("osm_line"));
            Coordinate[] lineString = new Coordinate[intersection.size()];
            for (int i = 0; i < intersection.size(); i++) {
                HashMap<String, Object> point = intersection.get(i);
                Coordinate coord = new Coordinate((double) point.get("lon"), (double) point.get("lat"));
                lineString[i] = coord;
            }
            double duration = (double) (endTime.getTime() - startTime.getTime()) / 1000;
            intersectionDelay.setDuration(duration);
            double length = geoService.getLength(List.of(lineString));
            intersectionDelay.setLength(length);
            intersectionDelay.setSpeed(3.6 * length/duration);
            intersectionDelay.setGeom(geometryFactory.createLineString(lineString));
            intersectionDelayRepository.save(intersectionDelay);
        }

        for (int i = 0; i < sortedPoints.size(); i++) {
            for (int j = 0; j < sortedPoints.get(i).size(); j++) {
                HashMap<String, Object> point = sortedPoints.get(i).get(j);
                Coordinate coord = new Coordinate((double) point.get("lon"), (double) point.get("lat"));
                MatchedPoint mP = new MatchedPoint();
                mP.setGeom(geometryFactory.createPoint(coord));
                mP.setEdgeId(i);
                mP.setPointInEdgeId(j);
                mP.setRide(ride);
                mP.setTimestamp((Date) point.get("timestamp"));
                if (point.get("osm_line") != null) {
                    mP.setLine((PlanetOsmLine) point.get("osm_line"));
                }
                mP.setInIntersection(point.get("intersection") != null);
                matchedPointRepository.save(mP);
            }
        }

        for (int i = 1; i < sortedPoints.size()-1; i++) {
            HashMap<String, Object> startPoint = sortedPoints.get(i).getFirst();
            HashMap<String, Object> endPoint = sortedPoints.get(i+1).getFirst();

            List<HashMap<String, Object>> points = new ArrayList<>(sortedPoints.get(i));
            points.add(endPoint);
            List<Coordinate> pointsCoordinates = new ArrayList<>();
            for (HashMap<String, Object> point : points) {
                pointsCoordinates.add(new Coordinate((double) point.get("lon"), (double) point.get("lat")));
            }
            Coordinate first = pointsCoordinates.getFirst();
            Coordinate last = pointsCoordinates.getLast();
            double lengthPoints = geoService.getLength(pointsCoordinates);

            if (startPoint.get("osm_line") != null) {
                PlanetOsmLine osmLine = (PlanetOsmLine) startPoint.get("osm_line");
                List<Coordinate> osmLineCoordinates = geoService.getLineCoordinates(osmLine);
                double lengthOsmLine = geoService.getLength(osmLineCoordinates);
                if (lengthOsmLine > 0 && lengthPoints/lengthOsmLine > 0.7) {
                    Date startTime = (Date) startPoint.get("timestamp");
                    Date endTime = (Date) endPoint.get("timestamp");

                    Edge edge = new Edge();
                    edge.setLine(osmLine);
                    edge.setRide(ride);
                    edge.setLength(lengthPoints);
                    edge.setStartTime(startTime);
                    edge.setEndTime(endTime);
                    edge.setSpeed(3.6 * lengthPoints/((double) (endTime.getTime() - startTime.getTime()) / 1000));

                    double dpFoF = geoService.distance(first, osmLineCoordinates.getFirst());
                    double dpFoL = geoService.distance(first, osmLineCoordinates.getLast());
                    double dpLoF = geoService.distance(last, osmLineCoordinates.getFirst());
                    double dpLoL = geoService.distance(last, osmLineCoordinates.getLast());
                    if (dpFoF < dpFoL && dpLoL < dpLoF && dpFoF < dpLoF) {
                        edge.setDirection(true);
                    } else if (dpFoL < dpFoF && dpLoF < dpFoF && dpFoL < dpLoL) {
                        edge.setDirection(false);
                    }
                    edgeRepository.save(edge);
                }
            }
        }

        _logger.info("Size of sorted: {}", sortedPoints.size());
	}

    private int distanceSignalOnHighway(String highway) {
        return switch (highway) {
            case "primary", "secondary", "cycleway", "path" -> 60;
            case "residential", "tertiary" -> 30;
            default -> 30;
        };
    }

    private int distanceMerge(String highway, String highway2) {
        if (highway.equals(highway2)) {
            return switch (highway) {
                case "primary", "secondary", "cycleway", "path" -> 60;
                case "residential", "tertiary" -> 30;
                default -> 30;
            };
        }
        return 30;
    }

    private Double getSmallestDistanceToTrafficSignals(List<TrafficSignal> trafficSignals, Coordinate coordinate) {
        List<Double> distances = new ArrayList<>();
        for (TrafficSignal trafficSignal : trafficSignals) {
            double distance = geoService.distance(coordinate, trafficSignal.getGeom().getCoordinate());
            distances.add(distance);
        }
        if (!distances.isEmpty()) {
            double smallestDistance = distances.getFirst();
            for (double distance : distances) {
                if (distance < smallestDistance) {
                    smallestDistance = distance;
                }
            }
            return smallestDistance;
        }
        return null;
    }

	public List<RidePoint> getRidePoints(Long rideId) {
		return ridePointRepository.findByRideId(rideId);
	}

	public List<MatchedPoint> getMatchedPoints(Long rideId) {
		return matchedPointRepository.findByRideId(rideId);
	}

    public List<IntersectionDelay> getIntersectionDelays(Long rideId) {
        return intersectionDelayRepository.findByRideId(rideId);
    }

    public List<IntersectionDelayGroup> aggregateDelays() {
        return intersectionDelayRepository.aggregateDelays();
    }

	public List<Edge> getEdges(Long rideId) {
		return edgeRepository.findByRideId(rideId);
	}

	public List<Long> getRideIds() {
		return rideRepository.getRideIds();
	}

    public List<EdgeSpeedStats> getAverageSpeeds() {
        return edgeRepository.getAvgSpeedByEdge();
    }

    public List<Long> findByOsmLineId(Long osmLineId) {
        return edgeRepository.findByOsmLineId(osmLineId);
    }
}
