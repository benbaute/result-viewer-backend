package com.simra.konsumgandalf.rides.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simra.konsumgandalf.common.logging.LogExecutionTime;
import com.simra.konsumgandalf.common.models.classes.MatchInformationDate;
import com.simra.konsumgandalf.common.models.classes.RideLoc;
import com.simra.konsumgandalf.common.models.dtos.IntersectionNodeAggregate;
import com.simra.konsumgandalf.common.models.dtos.IntersectionEdgeAggregate;
import com.simra.konsumgandalf.common.models.dtos.RegionAggregate;
import com.simra.konsumgandalf.common.models.entities.*;
import com.simra.konsumgandalf.common.repositories.PlanetOsmLineRepository;
import com.simra.konsumgandalf.common.utils.services.CsvUtilService;
import com.simra.konsumgandalf.common.utils.services.FileReaderService;
import com.simra.konsumgandalf.common.utils.services.GeoService;
import com.simra.konsumgandalf.common.services.OsmService;
import com.simra.konsumgandalf.osmPlanet.repositories.RegionRepository;
import com.simra.konsumgandalf.rides.repositories.*;
import com.simra.konsumgandalf.valhalla.services.ValhallaMapMatchingService;
import jakarta.transaction.Transactional;
import org.locationtech.jts.geom.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
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
    private IntersectionNodeRepository intersectionNodeRepository;

    @Autowired
    private IntersectionEdgeRepository intersectionEdgeRepository;

    @Autowired
    private RegionRepository regionRepository;

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
							saveRide(ride);
                            int count = counter.incrementAndGet();
							_logger.info("[" + count + "] Processed file: " + path);

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
            if (medianSpeeds.get(i) < 1 && i % 2 == 0) {
                // Remove slow points, as they are not needed,
                // but only every second point as they might contain relevant information over time
                coordinates.remove(i);
            }
        }

        if (coordinates.size() < 2) {
            throw new RuntimeException("Ride does not contain enough locations, after filtering low speeds.");
        }
        return coordinates;
    }

	public void saveRide(Ride ride) {
		ArrayList<MatchInformationDate> coordinates = getFilteredCoordinates(ride);
		Map<String, Object> traceAttributes = valhallaMapMatchingService.getTraceAttributes(coordinates);

		if (traceAttributes == null || traceAttributes.isEmpty() || traceAttributes.get("edges") == null
				|| traceAttributes.get("matched_points") == null) {
			throw new RuntimeException("Valhalla Error, Missing trace attributes for Ride");
		}

        // Enrich Valhalla Data
        List<HashMap<String, Object>> matchedPoints = this.getFilteredAndEnrichedPoints(traceAttributes, coordinates);
        List<HashMap<String, Object>> edges = this.getEnrichedEdges(traceAttributes);

        // Sort points by edge id
        List<List<HashMap<String, Object>>> sortedPoints = this.getSortedPointsAndEnrichPoints(matchedPoints, edges);
        List<HashMap<String, Object>> unsortedPoints = new ArrayList<>();
        for (List<HashMap<String, Object>> sortedPoint : sortedPoints) {
            unsortedPoints.addAll(sortedPoint);
        }

        this.saveRidePoints(ride, coordinates);
        List<List<List<HashMap<String, Object>>>> sortedRideParts = this.getRidePartsAndPutStops(sortedPoints);
        this.putIntersectionAndTrafficSignalCluster(unsortedPoints);
        this.saveMatchedPoints(ride, sortedPoints);
        List<IntersectionNode> intersectionNodeList = new ArrayList<>();
        List<IntersectionEdge> intersectionEdgeList = new ArrayList<>();
        for (List<List<HashMap<String, Object>>> sortedRidePart : sortedRideParts) {
            this.getIntersections(ride, sortedRidePart, intersectionEdgeList, intersectionNodeList);
        }

        // calculate waiting times
        List<Double> speeds = intersectionEdgeList.stream()
                .map(e -> e.getLength() / e.getDuration()).sorted().toList();
        if (!speeds.isEmpty()) {
            double medianSpeed = speeds.get(speeds.size() / 2);
            for  (IntersectionEdge intersectionEdge : intersectionEdgeList) {
                intersectionEdge.calculateAndSetWaitingTime(medianSpeed);
            }
            for  (IntersectionNode intersectionNode : intersectionNodeList) {
                intersectionNode.calculateAndSetWaitingTime(medianSpeed);
            }
        }
        intersectionEdgeRepository.saveAll(intersectionEdgeList);
        intersectionNodeRepository.saveAll(intersectionNodeList);
        _logger.info("Size of sorted: {}", sortedPoints.size());
	}

    public List<HashMap<String, Object>> getFilteredAndEnrichedPoints(Map<String, Object> traceAttributes,
                                                                      List<MatchInformationDate> coordinates) {
        ArrayList<HashMap<String, Object>> matchedPoints = (ArrayList<HashMap<String, Object>>) traceAttributes
                .get("matched_points");
        if (matchedPoints.size() != coordinates.size()) {
            throw new RuntimeException("Mismatch between matched Points and Ride locations");
        }
        for (int i = coordinates.size()-1; i >= 0; i--) {
            HashMap<String, Object> matchedPoint = matchedPoints.get(i);
            if (matchedPoint.get("error") != null) {
                // coordinates.remove(i);
                matchedPoints.remove(i);
            }
            else {
                matchedPoint.put("timestamp", coordinates.get(i).getOriginalTimestamp());
                Coordinate coordinate = new Coordinate((double) matchedPoint.get("lon"),
                        (double) matchedPoint.get("lat"));
                matchedPoint.put("coordinate", coordinate);
                matchedPoint.put("point", geometryFactory.createPoint(coordinate));
            }
        }
        return matchedPoints;
    }

    public List<HashMap<String, Object>> getEnrichedEdges(Map<String, Object> traceAttributes) {
        ArrayList<HashMap<String, Object>> edges = (ArrayList<HashMap<String, Object>>) traceAttributes.get("edges");
        for (HashMap<String, Object> edge : edges) {
            if (edge.get("way_id") != null) {
                Integer wayId = (Integer) edge.get("way_id");
                Optional<PlanetOsmLine> pLine = planetOsmLineRepository.findById(Long.valueOf(wayId));
                if (pLine.isPresent()) {
                    edge.put("osm_line", pLine.get());
                    List<TrafficSignalCluster> clusters = osmService.findTrafficSignalClustersByOsmLineId(Long.valueOf(wayId));
                    edge.put("traffic_signal_clusters", clusters);
                }
                else {
                    throw new RuntimeException("Could not find line with id " + wayId);
                }
            }
        }
        return edges;
    }

    /**
     * Sorts the points into lists with the same edge index.
     * Enriches the points with the following information based on the edge it belongs to:
     *  osm_line: edge
     *  way_id: if of edge
     *  traffic_signal_clusters: intersection of edge with traffic signal clusters
     * If no edge belongs to the points only the following information is added:
     *  traffic_signal_clusters: traffic signal clusters from edge before and after
     * If there is no edge for the first or last point group, those point groups are discarded.
     * If there is no edge for a group and the groups before and after have the same ids, those groups are also discarded.
     * @param points - The points from valhalla
     * @param edges - The edges from valhalla
     * @return - The sorted points.
     */
    public List<List<HashMap<String, Object>>> getSortedPointsAndEnrichPoints(List<HashMap<String, Object>> points,
                                                               List<HashMap<String, Object>> edges) {
        // Sort points by edge id
        List<List<HashMap<String, Object>>> sortedPoints = new ArrayList<>();
        List<HashMap<String, Object>> currentPoints = new ArrayList<>();
        Integer currentWayId = null;
        for (HashMap<String, Object> matchedPoint : points) {
            Integer wayId = null;
            if (matchedPoint.get("edge_index") != null) {
                int edgeIndex = (int) matchedPoint.get("edge_index");
                HashMap<String, Object> edge = edges.get(edgeIndex);
                wayId = (Integer) edge.get("way_id");
                matchedPoint.put("way_id", wayId);
                matchedPoint.put("osm_line", edge.get("osm_line"));
                matchedPoint.put("traffic_signal_clusters", edge.get("traffic_signal_clusters"));
            }
            if ((wayId == null && currentWayId != null) || (wayId != null && !wayId.equals(currentWayId))) {
                // If different way id, a new edge is created, and the current edge is saved
                currentWayId = wayId;
                if (!currentPoints.isEmpty()) {
                    sortedPoints.add(currentPoints);
                    currentPoints = new ArrayList<>();
                }
            }
            currentPoints.add(matchedPoint);
        }
        if (!currentPoints.isEmpty()) {
            sortedPoints.add(currentPoints);
            currentPoints = new ArrayList<>();
        }

        // Discard points without edge id, if at start or end
        // Discard points if in between same matching edge
        for (int i = sortedPoints.size()-1; i >= 0; i--) {
            Integer wayId = (Integer) sortedPoints.get(i).getFirst().get("way_id");
            if (wayId == null) {
                if (i == sortedPoints.size()-1 || i == 0) {
                    sortedPoints.remove(i);
                }
                else {
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

        // Put previous and next osm id
        for (int i = 0; i < sortedPoints.size(); i++) {
            Object prevLine = null;
            Object nextLine = null;
            if (i > 0) {
                prevLine = sortedPoints.get(i-1).getFirst().get("osm_line");
            }
            if (i < sortedPoints.size() - 1) {
                nextLine = sortedPoints.get(i+1).getFirst().get("osm_line");
            }
            for (HashMap<String, Object> p : sortedPoints.get(i)) {
                p.put("prev_osm_line", prevLine);
                p.put("next_osm_line", nextLine);
            }
        }

        // And traffic signal clusters for points without edge
        for (int i = 1; i < sortedPoints.size()-1; i++) {
            List<HashMap<String, Object>> current = sortedPoints.get(i);
            if (current.getFirst().get("way_id") == null) {
                List<TrafficSignalCluster> prev = (List<TrafficSignalCluster>)  sortedPoints.get(i-1).getLast().get("traffic_signal_clusters");
                List<TrafficSignalCluster> next = (List<TrafficSignalCluster>)  sortedPoints.get(i+1).getFirst().get("traffic_signal_clusters");
                List<TrafficSignalCluster> combined = new ArrayList<>();
                combined.addAll(prev);
                combined.addAll(next);
                for (HashMap<String, Object> p : current) {
                    p.put("traffic_signal_clusters", combined);
                }
            }
        }

        return sortedPoints;
    }

    /**
     * Adds whether a point belongs to an intersection, and which intersection it belongs to.
     * The point before and after are also added to the intersection
     * @param points - The enriched points from valhalla, each point must have a list of TrafficSignalCluster
     */
    public void putIntersectionAndTrafficSignalCluster(List<HashMap<String, Object>> points) {
        boolean inIntersection = false;
        for (int i = 0; i < points.size(); i++) {
            HashMap<String, Object> matchedPoint = points.get(i);
            Point point = (Point) matchedPoint.get("point");
            List<TrafficSignalCluster> clusters = (List<TrafficSignalCluster>) matchedPoint.get("traffic_signal_clusters");
            for (TrafficSignalCluster cluster : clusters) {
                if (geoService.pointInPolygon(point, cluster.getGeom())) {
                    matchedPoint.put("intersection", true);
                    matchedPoint.put("traffic_signal_cluster", cluster);
                }
            }
            if (matchedPoint.get("intersection") != null) {
                // Add point before in polygon also to intersection
                if (!inIntersection && i > 0) {
                    HashMap<String, Object> prevPoint = points.get(i-1);
                    prevPoint.put("intersection", true);
                }
                inIntersection = true;
            } else {
                // Add point after in polygon also to intersection
                if (inIntersection) {
                    matchedPoint.put("intersection", true);
                }
                inIntersection = false;
            }
        }
        for (int i = 0; i < points.size(); i++) {
            HashMap<String, Object> point = points.get(i);
            // Put cluster to intersections without cluster
            if (point.get("intersection") != null && point.get("traffic_signal_cluster") == null) {
                // If in intersection, but not belonging to a cluster, move it to belonging cluster
                if (i < points.size()-1) {
                    // Point belongs to cluster following it, in case it exists
                    HashMap<String, Object> nextPoint = points.get(i+1);
                    point.put("traffic_signal_cluster", nextPoint.get("traffic_signal_cluster"));
                }
                if (i > 0 && point.get("traffic_signal_cluster") == null) {
                    // Else point belongs to cluster before it
                    HashMap<String, Object> prevPoint = points.get(i-1);
                    point.put("traffic_signal_cluster", prevPoint.get("traffic_signal_cluster"));
                }
            }
        }
    }


    /**
     * Splits a ride into multiple parts.
     * It adds a stop if the distance behaves unexpectantly.
     * The edge containing the stop is not added to any ride part.
     * @param sortedPoints - The enriched list of lists of points from valhalla, each point must have a Coordinate.
     *                     The inner list contains all points with the same edge index (if in order) = edge
     *                     The outer list contains all edges of the ride.
     * @return - The ride parts.
     */
    public List<List<List<HashMap<String, Object>>>> getRidePartsAndPutStops(List<List<HashMap<String, Object>>> sortedPoints) {
        List<List<List<HashMap<String, Object>>>> sortedRideParts = new ArrayList<>();
        if (sortedPoints.isEmpty()) {
            return sortedRideParts;
        }
        List<List<HashMap<String, Object>>> currentRouteParts = new ArrayList<>();

        Coordinate previousCoordinate = (Coordinate) sortedPoints.getFirst().getFirst().get("coordinate");
        int sizeLoopCheck = 5;
        int stops = 0;
        int edgeId = 0;
        for (List<HashMap<String, Object>> edge : sortedPoints) {
            if (edgeId == 81) {
                edgeId = edgeId;
            }
            edgeId++;
            Date t0 = (Date) edge.getFirst().get("timestamp");
            double maxDistanceFromTracePoint = 0;
            boolean foundStop = false;
            for (int i = 0; i < edge.size(); i++) {
                HashMap<String, Object> currentPoint = edge.get(i);
                currentPoint.put("stops", stops);
                Coordinate currentCoordinate = (Coordinate) currentPoint.get("coordinate");
                Coordinate compareCoordinatePrevious = currentCoordinate;
                List<Double> distances = new ArrayList<>();
                for (int j = 1; i + j < edge.size() && distances.size() < sizeLoopCheck; j++) {
                    Coordinate compareCoordinate = (Coordinate) edge.get(i + j).get("coordinate");
                    if (compareCoordinate.equals(compareCoordinatePrevious)) {
                        continue;
                    }
                    distances.add(geoService.distance(currentCoordinate, compareCoordinate));
                    compareCoordinatePrevious = compareCoordinate;
                }
                for (int j = 0; j < distances.size() - 1; j++) {
                    if (distances.get(j) - distances.get(j + 1) > 2) {
                        // Add stop, if distance did not increase on current edge (considering threshold of 2 meters)
                        // This can lead to false stop detections on winding roads (or on roads without osm detection)
                        foundStop = true;
                        break;
                    }
                }

                if (geoService.distance(currentCoordinate, previousCoordinate) > 100) {
                    // Add stop if distance to previous point is exceeding 100 meters
                    // This should only happen dur to bad GPS tracking/matching
                    foundStop = true;
                }
                previousCoordinate = currentCoordinate;

                Double distance_from_trace_point = (Double) currentPoint.get("distance_from_trace_point");
                if (distance_from_trace_point != null && distance_from_trace_point > maxDistanceFromTracePoint) {
                    maxDistanceFromTracePoint = distance_from_trace_point;
                }

                Date t1 = (Date) currentPoint.get("timestamp");
                long diff = (t1.getTime() - t0.getTime())/1000;
                if (diff > 60 * 5 || (maxDistanceFromTracePoint > 15 && diff > 60 * 2)) {
                    // Add stop if an edge takes longer then 5 minutes to complete
                    // Or if there is a break of 2 minutes and a large distance from trace point on current edge
                    // indicating a short break, leaving the current path
                    foundStop = true;
                }
            }
            if (!foundStop) {
                // Adds, only parts without stops
                currentRouteParts.add(edge);
            } else {
                stops++;
                this.removeFirstAndLastAndServiceWays(currentRouteParts);
                // If a stop occurred, all previous edges are placed into one list.
                if (!currentRouteParts.isEmpty()) {
                    sortedRideParts.add(currentRouteParts);
                    currentRouteParts = new ArrayList<>();
                }
            }
        }
        this.removeFirstAndLastAndServiceWays(currentRouteParts);
        if (!currentRouteParts.isEmpty()) {
            sortedRideParts.add(currentRouteParts);
            currentRouteParts = new ArrayList<>();
        }
        return sortedRideParts;
    }

    public void removeFirstAndLastAndServiceWays(List<List<HashMap<String, Object>>> routeParts) {
        this.removeServiceWays(routeParts);
        if (!routeParts.isEmpty()) {
            routeParts.removeFirst();
        }
        if (!routeParts.isEmpty()) {
            routeParts.removeLast();
        }
        this.removeServiceWays(routeParts);
    }

    public boolean checkEndStartCondition(List<HashMap<String, Object>> edge) {
        PlanetOsmLine line = (PlanetOsmLine) edge.getFirst().get("osm_line");
        if (line != null && line.getHighway().equals("service")) {
            // If edge is a service way, remove service way (as this is usually unintended ride)
            return  true;
        }

        for (HashMap<String, Object> currentPoint : edge) {
            Double distance_from_trace_point = (Double) currentPoint.get("distance_from_trace_point");
            if (distance_from_trace_point != null && distance_from_trace_point > 15) {
                // If the edge contains a point with a large distance from the trace point, the current position
                // is usually wrongly mapped as the position is most likely not on the path but inside a house
                return true;
            }
        }
        return false;
    }

    public void removeServiceWays(List<List<HashMap<String, Object>>> routeParts) {
        while (!routeParts.isEmpty()) {
            if (checkEndStartCondition(routeParts.getFirst())) {
                routeParts.removeFirst();
            } else {
                break;
            }
        }
        while (!routeParts.isEmpty()) {
            if (checkEndStartCondition(routeParts.getLast())) {
                routeParts.removeLast();
            } else {
                break;
            }
        }
    }

    public void getIntersections(Ride ride, List<List<HashMap<String, Object>>> sortedPoints,
                                 List<IntersectionEdge> intersectionEdgeList, List<IntersectionNode> intersectionNodeList) {
        if (sortedPoints.isEmpty() || sortedPoints.size() < 2) {
            return;
        }
        List<HashMap<String, Object>> unsortedPoints = new ArrayList<>();
        for (List<HashMap<String, Object>> edge : sortedPoints) {
            unsortedPoints.addAll(edge);
        }

        // Create intersections
        List<List<HashMap<String, Object>>> allEdges = new ArrayList<>();
        List<HashMap<String, Object>> currentEdge = new ArrayList<>();

        List<List<HashMap<String, Object>>> allIntersections = new ArrayList<>();
        List<HashMap<String, Object>> currentIntersection = new ArrayList<>();
        TrafficSignalCluster currentCluster = null;
        for (HashMap<String, Object> point : unsortedPoints) {
            if (point.get("intersection") != null) {
                TrafficSignalCluster cluster = (TrafficSignalCluster) point.get("traffic_signal_cluster");
                if (currentCluster != null && !Objects.equals(cluster.getId(), currentCluster.getId())) {
                    if (!currentIntersection.isEmpty()) {
                        allIntersections.add(currentIntersection);
                        currentIntersection = new ArrayList<>();
                    }
                }
                currentCluster = cluster;
                currentIntersection.add(point);
                if (!currentEdge.isEmpty()) {
                    // Add first point of intersection to remove gap
                    currentEdge.add(point);
                    allEdges.add(currentEdge);
                    currentEdge = new ArrayList<>();
                }
            } else {
                if (!currentIntersection.isEmpty()) {
                    currentEdge.add(currentIntersection.getLast()); // Add last point of intersection to remove gap
                    allIntersections.add(currentIntersection);
                    currentIntersection = new ArrayList<>();
                }
                currentEdge.add(point);
            }
        }
        if (!currentEdge.isEmpty()) {
            allEdges.add(currentEdge);
        }
        if (!currentIntersection.isEmpty()) {
            allIntersections.add(currentIntersection);
        }

        // Skips first intersection if ride starts with it as that is likely incomplete.
        // The last intersection is skipped as well if the ride ends with it
        if (unsortedPoints.getFirst().get("intersection") != null) {
            allIntersections.removeFirst();
        }
        if (unsortedPoints.getLast().get("intersection") != null) {
            if (!allIntersections.isEmpty()) {
                allIntersections.removeLast();
            }
        }

        for (List<HashMap<String, Object>> edge : allEdges) {
            List<List<HashMap<String, Object>>> sortedEdges = new ArrayList<>();
            List<HashMap<String, Object>> sortedEdge = new ArrayList<>();
            Integer wayId = (Integer) edge.getFirst().get("way_id");
            for (HashMap<String, Object> point : edge) {
                Integer currentWayId = (Integer) point.get("way_id");
                sortedEdge.add(point);
                if ((wayId == null && currentWayId != null) || (wayId != null && !wayId.equals(currentWayId))) {
                    wayId = currentWayId;
                    sortedEdges.add(sortedEdge);
                    sortedEdge = new ArrayList<>();
                    sortedEdge.add(point); // Add point to both edges to remove gaps between edges
                }
            }
            if (!sortedEdge.isEmpty()) {
                sortedEdges.add(sortedEdge);
            }

            for (List<HashMap<String, Object>> e : sortedEdges) {
                if (e.size() < 2) {
                    continue;
                }
                IntersectionEdge intersectionEdge = new IntersectionEdge();
                this.applyIntersectionProperties(e, intersectionEdge, ride);

                intersectionEdge.setLine((PlanetOsmLine) e.getFirst().get("osm_line"));
                intersectionEdge.setPrevLine((PlanetOsmLine) e.getFirst().get("prev_osm_line"));
                intersectionEdge.setNextLine((PlanetOsmLine) e.getFirst().get("next_osm_line"));
                intersectionEdgeList.add(intersectionEdge);
            }
        }

        for (List<HashMap<String, Object>> intersection : allIntersections) {
            if (intersection.size() < 2) {
                continue;
            }
            IntersectionNode intersectionNode = new IntersectionNode();
            this.applyIntersectionProperties(intersection, intersectionNode, ride);

            TrafficSignalCluster cluster = (TrafficSignalCluster) intersection.get(1).get("traffic_signal_cluster");
            intersectionNode.setTrafficSignalCluster(cluster);
            intersectionNode.setEndLine((PlanetOsmLine) intersection.getLast().get("osm_line"));
            intersectionNode.setStartLine((PlanetOsmLine) intersection.getFirst().get("osm_line"));

            List<String> names = new ArrayList<>();
            for (HashMap<String, Object> point : intersection) {
                PlanetOsmLine line = (PlanetOsmLine) point.get("osm_line");
                if (line != null && line.getName() != null && (names.isEmpty() || !names.getLast().equals(line.getName()))) {
                    names.add(line.getName());
                }
            }
            if (names.isEmpty()) {
                List<String> namesCluster = cluster.getOsmLinesName();
                for (int i = 0; namesCluster != null && i < namesCluster.size()  && i < 2; i++) {
                    names.add(namesCluster.get(i));
                }
            }
            intersectionNode.setStreetNames(String.join(";", names));

            intersectionNodeList.add(intersectionNode);
        }
    }

    public void applyIntersectionProperties(List<HashMap<String, Object>> intersection, IntersectionBaseClass element, Ride ride) {
        Date endTime = (Date) intersection.getLast().get("timestamp");
        Date startTime = (Date) intersection.getFirst().get("timestamp");
        element.setEndTime(endTime);
        element.setStartTime(startTime);
        double duration = (double) (endTime.getTime() - startTime.getTime()) / 1000;
        element.setDuration(duration);
        element.setRide(ride);
        Coordinate[] lineString = new Coordinate[intersection.size()];
        List<String> names = new ArrayList<>();
        for (int i = 0; i < intersection.size(); i++) {
            HashMap<String, Object> point = intersection.get(i);
            PlanetOsmLine line = (PlanetOsmLine) point.get("osm_line");
            if (line != null && line.getName() != null && (names.isEmpty() || !names.getLast().equals(line.getName()))) {
                names.add(line.getName());
            }
            lineString[i] = (Coordinate) point.get("coordinate");
        }
        double length = geoService.getLength(List.of(lineString));
        element.setLength(length);
        element.setSpeed(3.6 * length/duration);
        element.setGeom(geometryFactory.createLineString(lineString));
        element.setRegions(new HashSet<>(regionRepository
                .findContainingRegions((Point) intersection.getFirst().get("point"))));
    }

    public void saveMatchedPoints(Ride ride, List<List<HashMap<String, Object>>> sortedPoints) {
        List<MatchedPoint> matchedPointList = new ArrayList<>();
        for (int i = 0; i < sortedPoints.size(); i++) {
            for (int j = 0; j < sortedPoints.get(i).size(); j++) {
                HashMap<String, Object> point = sortedPoints.get(i).get(j);
                MatchedPoint mP = new MatchedPoint();
                mP.setGeom((Point) point.get("point"));
                mP.setEdgeId(i);
                mP.setPointInEdgeId(j);
                mP.setRide(ride);
                mP.setTimestamp((Date) point.get("timestamp"));
                if (point.get("osm_line") != null) {
                    mP.setLine((PlanetOsmLine) point.get("osm_line"));
                }
                mP.setInIntersection(point.get("intersection") != null);
                mP.setDistanceAlongEdge((Double) point.get("distance_along_edge"));
                mP.setDistanceFromTracePoint((Double) point.get("distance_from_trace_point"));
                mP.setStops((int)  point.get("stops"));
                matchedPointList.add(mP);
            }
        }
        matchedPointRepository.saveAll(matchedPointList);
    }

    public void saveRidePoints(Ride ride, List<MatchInformationDate> coordinates) {
        List<RidePoint> ridePointList = new ArrayList<>();
        for (MatchInformationDate loc : coordinates) {
            RidePoint p = new RidePoint();
            p.setRide(ride);
            p.setTimestamp(loc.getOriginalTimestamp());
            Coordinate coord = new Coordinate(loc.getLng(), loc.getLat());
            p.setGeom(geometryFactory.createPoint(coord));
            ridePointList.add(p);
        }
        ridePointRepository.saveAll(ridePointList);
    }


	public List<RidePoint> getRidePoints(Long rideId) {
		return ridePointRepository.findByRideId(rideId);
	}

	public List<MatchedPoint> getMatchedPoints(Long rideId) {
		return matchedPointRepository.findByRideId(rideId);
	}

    public List<IntersectionNode> getIntersectionNodes(Long rideId) {
        return intersectionNodeRepository.findByRideId(rideId);
    }

    public List<IntersectionEdge> getIntersectionEdge(Long rideId) {
        return intersectionEdgeRepository.findByRideId(rideId);
    }

    public List<IntersectionNode> getIntersectionNodes(Long trafficSignalClusterId, Long startOsmId, Long endOsmId) {
        return intersectionNodeRepository.findByClusterIdStartEndOsmId(trafficSignalClusterId, startOsmId, endOsmId);
    }

    public List<IntersectionNodeAggregate> aggregateNodes(Long trafficSignalClusterId, Long count, String region,
                                                          String streetNames) {
        return intersectionNodeRepository.aggregateNodes(trafficSignalClusterId, count, region, streetNames);
    }

    public List<String> findAllStreetNamesIncludingStringIntersectionNode(Long trafficSignalClusterId,
              Long count, String region, String streetNames) {
        return intersectionNodeRepository.findAllIncludingString(trafficSignalClusterId, count, region, streetNames);
    }

    public List<IntersectionEdge> getIntersectionEdge(Long prevOsmId, Long osmId, Long nextOsmId) {
        return intersectionEdgeRepository.findByPrevIdOsmIdNext(prevOsmId, osmId, nextOsmId);
    }

    public List<IntersectionEdgeAggregate> aggregateEdges(Long count, String region, String name) {
        return intersectionEdgeRepository.aggregateEdges(count, region, name);
    }

    public List<String> findAllStreetNamesIntersectionEdge(Long count, String region, String name) {
        return intersectionEdgeRepository.findAllStreetNames(count, region, name);
    }

	public List<Long> getRideIds() {
		return rideRepository.getRideIds();
	}

    public List<Long> findByOsmLineId(Long osmLineId) {
        return intersectionEdgeRepository.findByOsmLineId(osmLineId);
    }

    public List<RegionAggregate> aggregateIntersectionDataPerRegion(String region) {
        return regionRepository.aggregateIntersectionDataPerRegion(region);
    }

    public List<Region> findRegionByName(String region) {
        return regionRepository.findRegionByName(region);
    }
}
