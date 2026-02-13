package com.simra.konsumgandalf.rides.services;

import com.simra.konsumgandalf.common.logging.LogExecutionTime;
import com.simra.konsumgandalf.common.logging.LogExecutionTimeSubTask;
import com.simra.konsumgandalf.common.models.classes.Edge;
import com.simra.konsumgandalf.common.models.classes.MatchInformation;
import com.simra.konsumgandalf.common.models.dtos.IntersectionEdgeAggregate;
import com.simra.konsumgandalf.common.models.dtos.IntersectionNodeAggregate;
import com.simra.konsumgandalf.common.models.dtos.RegionAggregate;
import com.simra.konsumgandalf.common.models.entities.*;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.common.repositories.PlanetOsmLineRepository;
import com.simra.konsumgandalf.common.services.OsmService;
import com.simra.konsumgandalf.common.utils.services.GeoService;
import com.simra.konsumgandalf.osmPlanet.repositories.RegionRepository;
import com.simra.konsumgandalf.rides.classes.specifications.IntersectionEdgeMetricsSpecifications;
import com.simra.konsumgandalf.rides.classes.specifications.IntersectionNodeMetricsSpecifications;
import com.simra.konsumgandalf.rides.repositories.*;
import com.simra.konsumgandalf.valhalla.models.TraceResponse;
import com.simra.konsumgandalf.valhalla.models.ValhallaEdge;
import com.simra.konsumgandalf.valhalla.models.ValhallaMatchedPoint;
import com.simra.konsumgandalf.valhalla.services.ValhallaMapMatchingService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RideService {

	private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

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
    private IntersectionNodeMetricsRepository intersectionNodeMetricsRepository;

    @Autowired
    private IntersectionEdgeRepository intersectionEdgeRepository;

    @Autowired
    private IntersectionEdgeMetricsRepository intersectionEdgeMetricsRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private GeoService geoService;

    @Autowired
    private OsmService osmService;

	@Autowired
	private ValhallaMapMatchingService valhallaMapMatchingService;

	RideService() {}

    @LogExecutionTimeSubTask
    public void processRideEntity (RideEntity rideEntity) {
        Ride ride = new Ride(rideEntity.getPath());
        ride.setCoordinates(rideEntity.getCleanLocations());
        saveRide(ride);
    }

	private void saveRide(Ride ride) {
        rideRepository.save(ride);
        saveRidePoints(ride); // raw GPS points

        // Sort points by edge osm id
        List<List<MatchedPoint>> sortedPoints = getSortedPointsAndEnrichPoints(
                ride, valhallaMapMatchingService.getTraceAttributes(ride.getCoordinates()));

        List<MatchedPoint> unsortedPoints = new ArrayList<>();
        for (List<MatchedPoint> sortedPoint : sortedPoints) {
            unsortedPoints.addAll(sortedPoint);
        }

        if (unsortedPoints.isEmpty()) {
            return; // No successfully matched points, so no further calculation required
        }

        putIntersectionAndTrafficSignalCluster(unsortedPoints);
        List<List<List<MatchedPoint>>> sortedRideParts = getRidePartsAndPutStops(sortedPoints);
        saveMatchedPoints(sortedPoints);

        List<IntersectionNode> intersectionNodeList = new ArrayList<>();
        List<IntersectionEdge> intersectionEdgeList = new ArrayList<>();
        for (List<List<MatchedPoint>> sortedRidePart : sortedRideParts) {
            getIntersections(ride, sortedRidePart, intersectionEdgeList, intersectionNodeList);
        }
        setWaitingTimes(intersectionNodeList, intersectionEdgeList);

        setContainingRegions(intersectionEdgeList);
        setContainingRegions(intersectionNodeList);

        intersectionEdgeRepository.saveAll(intersectionEdgeList);
        intersectionNodeRepository.saveAll(intersectionNodeList);
	}

    private List<MatchedPoint> getEnrichedPoints(Ride ride, TraceResponse traceResponse) throws IllegalArgumentException {
        List<MatchedPoint> matchedPoints = new ArrayList<>();

        if (traceResponse.getMatchedPoints().size() != traceResponse.getPayloadCoordinates().size()) {
            throw new IllegalArgumentException("Mismatch between matched Points and Ride locations");
        }
        for (int i = 0; i < traceResponse.getMatchedPoints().size(); i++) {
            ValhallaMatchedPoint p = traceResponse.getMatchedPoints().get(i);
            MatchedPoint matchedPoint = new MatchedPoint();

            Coordinate coordinate = new Coordinate(p.getLon(), p.getLat());

            matchedPoint.setRide(ride);
            matchedPoint.setEdgeIndex(p.getEdgeIndex());
            matchedPoint.setCoordinate(coordinate);
            matchedPoint.setGeom(geometryFactory.createPoint(coordinate));
            matchedPoint.setTimestamp( new Date(traceResponse.getPayloadCoordinates().get(i).getTimestamp() * 1000));
            matchedPoint.setDistanceFromTracePoint(p.getDistanceFromTracePoint());

            matchedPoints.add(matchedPoint);
        }
        return matchedPoints;
    }

    private List<Edge> getEnrichedEdges(TraceResponse traceResponse) throws IllegalArgumentException {
        List<Edge> edges = new ArrayList<>();
        List<Long> wayIds = traceResponse.getEdges().stream()
                .map(ValhallaEdge::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Object[]> resultsTrafficSignalClusters = osmService.findTrafficSignalClustersByOsmLineIds(wayIds);
        Map<Long, List<TrafficSignalCluster>> clusterMap = new HashMap<>();
        for (Object[] row : resultsTrafficSignalClusters) {
            Long osmId = ((Number) row[0]).longValue();
            TrafficSignalCluster cluster = (TrafficSignalCluster) row[1];

            clusterMap
                    .computeIfAbsent(osmId, k -> new ArrayList<>())
                    .add(cluster);
        }

        List<PlanetOsmLine> resultsLines = planetOsmLineRepository.findByIds(wayIds);
        Map<Long, PlanetOsmLine> linesMap = new HashMap<>();
        for (PlanetOsmLine line : resultsLines) {
            linesMap.put(line.getId(), line);
        }

        for (ValhallaEdge valhallaEdge : traceResponse.getEdges()) {
            Edge edge = new Edge();

            Long wayId = valhallaEdge.getId();
            if (linesMap.containsKey(wayId)) {
                edge.setOsmId(wayId);
                edge.setOsmLine(linesMap.get(wayId));
            } else {
                throw new IllegalArgumentException("Could not find line with id " + wayId);
            }

            edge.setTrafficSignalClusters(clusterMap.containsKey(wayId) ? clusterMap.get(wayId) : new ArrayList<>());
            edges.add(edge);
        }

        return edges;
    }

    /**
     * Sorts the points into lists with the same edge osm id.
     * Enriches the points with the following information based on the edge it belongs to:
     *  osm_line: edge
     *  way_id: if of edge
     *  traffic_signal_clusters: intersection of edge with traffic signal clusters
     * If no edge belongs to the points only the following information is added:
     *  traffic_signal_clusters: traffic signal clusters from edge before and after
     * If there is no edge for the first or last point group, those point groups are discarded.
     * If there is no edge for a group and the groups before and after have the same ids, those groups are also discarded.
     * @param ride - The current ride
     * @param traceResponse - The points and edges from valhalla
     * @return - The sorted points.
     */
    private List<List<MatchedPoint>> getSortedPointsAndEnrichPoints(Ride ride, TraceResponse traceResponse) throws IllegalArgumentException {
        // Enrich Valhalla Data
        List<MatchedPoint> points = getEnrichedPoints(ride, traceResponse);
        List<Edge> edges = getEnrichedEdges(traceResponse);

        // Sort points by edge osm id
        List<List<MatchedPoint>> sortedPoints = new ArrayList<>();
        List<MatchedPoint> currentPoints = new ArrayList<>();
        Long currentWayId = null;
        for (MatchedPoint matchedPoint : points) {
            Long wayId = null;
            if (matchedPoint.getEdgeIndex() != null) {
                int edgeIndex = matchedPoint.getEdgeIndex();
                Edge edge = edges.get(edgeIndex);
                wayId = edge.getOsmId();
                matchedPoint.setOsmId(wayId);
                matchedPoint.setLine(edge.getOsmLine());
                matchedPoint.setTrafficSignalClusters(edge.getTrafficSignalClusters());
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

        // Discard points without edge osm id, if at start or end
        for (int i = sortedPoints.size()-1; i >= 0; i--) {
            Long wayId = sortedPoints.get(i).getFirst().getOsmId();
            if (wayId == null) {
                if (i == sortedPoints.size()-1 || i == 0) {
                    sortedPoints.remove(i);
                }
                else {
                    Long prevWayId = sortedPoints.get(i-1).getFirst().getOsmId();
                    Long nextWayId = sortedPoints.get(i+1).getFirst().getOsmId();
                    if (prevWayId == null) {
                        sortedPoints.get(i-1).addAll(sortedPoints.get(i));
                        sortedPoints.remove(i);
                    } else if (nextWayId == null) {
                        throw new IllegalArgumentException("Unexpected  null error.");
                    } else if (prevWayId.equals(nextWayId)) {
                        // Merge points into one edge if in between same matching edge
                        sortedPoints.get(i-1).addAll(sortedPoints.get(i));
                        sortedPoints.get(i-1).addAll(sortedPoints.get(i+1));
                        sortedPoints.remove(i+1);
                        sortedPoints.remove(i);
                    }
                }
            } else {
                if (i > 0) {
                    Long prevWayId = sortedPoints.get(i-1).getFirst().getOsmId();
                    if (wayId.equals(prevWayId)) {
                        sortedPoints.get(i-1).addAll(sortedPoints.get(i));
                        sortedPoints.remove(i);
                    }
                }
            }
        }

        // Put previous and next osm id
        for (int i = 0; i < sortedPoints.size(); i++) {
            PlanetOsmLine prevLine = null;
            PlanetOsmLine nextLine = null;
            if (i > 0) {
                prevLine = sortedPoints.get(i-1).getFirst().getLine();
            }
            if (i < sortedPoints.size() - 1) {
                nextLine = sortedPoints.get(i+1).getFirst().getLine();
            }
            for (MatchedPoint p : sortedPoints.get(i)) {
                p.setPrevLine(prevLine);
                p.setNextLine(nextLine);
            }
        }

        // And traffic signal clusters for points without edge
        for (int i = 1; i < sortedPoints.size()-1; i++) {
            List<MatchedPoint> current = sortedPoints.get(i);
            if (current.getFirst().getOsmId() == null) {
                List<TrafficSignalCluster> prev = sortedPoints.get(i-1).getLast().getTrafficSignalClusters();
                List<TrafficSignalCluster> next = sortedPoints.get(i+1).getFirst().getTrafficSignalClusters();
                List<TrafficSignalCluster> combined = new ArrayList<>();
                combined.addAll(prev);
                combined.addAll(next);
                for (MatchedPoint p : current) {
                    p.setTrafficSignalClusters(combined);
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
    private void putIntersectionAndTrafficSignalCluster(List<MatchedPoint> points) {
        boolean inIntersection = false;
        for (int i = 0; i < points.size(); i++) {
            MatchedPoint matchedPoint = points.get(i);
            Point point = matchedPoint.getGeom();
            List<TrafficSignalCluster> clusters = matchedPoint.getTrafficSignalClusters();
            if (clusters != null) {
                for (TrafficSignalCluster cluster : clusters) {
                    if (geoService.pointInPolygon(point, cluster.getGeom())) {
                        matchedPoint.setInIntersection(true);
                        matchedPoint.setInIntersectionCluster(cluster);
                    }
                }
            }
            if (matchedPoint.getInIntersection()) {
                // Add point before in polygon also to intersection
                if (!inIntersection && i > 0) {
                    MatchedPoint prevPoint = points.get(i-1);
                    prevPoint.setInIntersection(true);
                }
                inIntersection = true;
            } else {
                // Add point after in polygon also to intersection
                if (inIntersection) {
                    matchedPoint.setInIntersection(true);
                }
                inIntersection = false;
            }
        }
        for (int i = 0; i < points.size(); i++) {
            MatchedPoint point = points.get(i);
            // Put cluster to intersections without cluster
            if (point.getInIntersection() && point.getInIntersectionCluster() == null) {
                // If in intersection, but not belonging to a cluster, move it to belonging cluster
                if (i < points.size()-1) {
                    // Point belongs to cluster following it, in case it exists
                    MatchedPoint nextPoint = points.get(i+1);
                    point.setInIntersectionCluster(nextPoint.getInIntersectionCluster());
                }
                if (i > 0 && point.getInIntersectionCluster() == null) {
                    // Else point belongs to cluster before it
                    MatchedPoint prevPoint = points.get(i-1);
                    point.setInIntersectionCluster(prevPoint.getInIntersectionCluster());
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
    private List<List<List<MatchedPoint>>> getRidePartsAndPutStops(List<List<MatchedPoint>> sortedPoints) {
        List<List<List<MatchedPoint>>> sortedRideParts = new ArrayList<>();
        if (sortedPoints.isEmpty()) {
            return sortedRideParts;
        }
        List<List<MatchedPoint>> currentRouteParts = new ArrayList<>();

        List<MatchedPoint> pointsInRage = new ArrayList<>();
        Coordinate previousCoordinate = sortedPoints.getFirst().getFirst().getCoordinate();
        int sizeLoopCheck = 5;
        int stops = 0;


        for (List<MatchedPoint> edge : sortedPoints) {
            boolean foundStop = false;
            for (int i = 0; i < edge.size(); i++) {
                MatchedPoint currentPoint = edge.get(i);
                currentPoint.setStops(stops);
                Coordinate currentCoordinate = currentPoint.getCoordinate();
                Coordinate compareCoordinatePrevious = currentCoordinate;
                List<Double> distances = new ArrayList<>();
                for (int j = 1; i + j < edge.size() && distances.size() < sizeLoopCheck; j++) {
                    Coordinate compareCoordinate = (Coordinate) edge.get(i + j).getCoordinate();
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
                    // This should only happen due to bad GPS tracking/matching
                    foundStop = true;
                }
                previousCoordinate = currentCoordinate;

                while (!pointsInRage.isEmpty() && geoService.distance(currentCoordinate,
                        pointsInRage.getFirst().getCoordinate()) > 50) {
                    pointsInRage.removeFirst(); // remove points with larger distance
                }
                if (!pointsInRage.isEmpty()) {
                    double maxDistanceFromTracePoint = 0;
                    for (MatchedPoint point : pointsInRage) {
                        Double distance_from_trace_point = point.getDistanceFromTracePoint();
                        if (distance_from_trace_point != null && distance_from_trace_point > maxDistanceFromTracePoint) {
                            maxDistanceFromTracePoint = distance_from_trace_point;
                        }
                    }
                    long diffToPointInRage = ((currentPoint.getTimestamp()).getTime() -
                            (pointsInRage.getFirst().getTimestamp()).getTime())/1000;
                    boolean inIntersection = currentPoint.getInIntersection();
                    if ((inIntersection && diffToPointInRage > 60 * 4)
                            || (!inIntersection && diffToPointInRage > 60 * 2)
                            || (maxDistanceFromTracePoint > 15 && diffToPointInRage > 60 * 2)) {
                        // Add stop if not moved more than 50 meters
                        // ... in the last 4 minutes and in intersection
                        // ... in the last 2 minutes and not in intersection
                        // ... in the last 2 minutes and a large distance from trace point (in range) indicating a short break
                        foundStop = true;
                    }
                }
                pointsInRage.add(currentPoint);
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

    private void removeFirstAndLastAndServiceWays(List<List<MatchedPoint>> routeParts) {
        this.removeServiceWays(routeParts);
        if (!routeParts.isEmpty()) {
            routeParts.removeFirst();
        }
        if (!routeParts.isEmpty()) {
            routeParts.removeLast();
        }
        this.removeServiceWays(routeParts);
    }

    private boolean checkEndStartCondition(List<MatchedPoint> edge) {
        PlanetOsmLine line = edge.getFirst().getLine();
        if (line != null && line.getHighway().equals("service")) {
            // If edge is a service way, remove service way (as this is usually unintended ride)
            return  true;
        }

        for (MatchedPoint currentPoint : edge) {
            Double distance_from_trace_point = currentPoint.getDistanceFromTracePoint();
            if (distance_from_trace_point != null && distance_from_trace_point > 15) {
                // If the edge contains a point with a large distance from the trace point, the current position
                // is usually wrongly mapped as the position is most likely not on the path but inside a house
                return true;
            }
        }
        return false;
    }

    private void removeServiceWays(List<List<MatchedPoint>> routeParts) {
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

    private void getIntersections(Ride ride, List<List<MatchedPoint>> sortedPoints,
                                 List<IntersectionEdge> intersectionEdgeList, List<IntersectionNode> intersectionNodeList) {
        if (sortedPoints.isEmpty() || sortedPoints.size() < 2) {
            return;
        }
        List<MatchedPoint> unsortedPoints = new ArrayList<>();
        for (List<MatchedPoint> edge : sortedPoints) {
            unsortedPoints.addAll(edge);
        }

        // Create intersections
        List<List<MatchedPoint>> allEdges = new ArrayList<>();
        List<MatchedPoint> currentEdge = new ArrayList<>();

        List<List<MatchedPoint>> allIntersections = new ArrayList<>();
        List<MatchedPoint> currentIntersection = new ArrayList<>();
        TrafficSignalCluster currentCluster = null;
        for (MatchedPoint point : unsortedPoints) {
            if (point.getInIntersection()) {
                TrafficSignalCluster cluster = point.getInIntersectionCluster();
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
        if (unsortedPoints.getFirst().getInIntersection()) {
            allIntersections.removeFirst();
        }
        if (unsortedPoints.getLast().getInIntersection()) {
            if (!allIntersections.isEmpty()) {
                allIntersections.removeLast();
            }
        }

        for (List<MatchedPoint> edge : allEdges) {
            List<List<MatchedPoint>> sortedEdges = new ArrayList<>();
            List<MatchedPoint> sortedEdge = new ArrayList<>();
            Long wayId = edge.getFirst().getOsmId();
            for (MatchedPoint point : edge) {
                Long currentWayId = point.getOsmId();
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

            for (List<MatchedPoint> e : sortedEdges) {
                if (e.size() < 2) {
                    continue;
                }
                IntersectionEdge intersectionEdge = new IntersectionEdge();
                applyIntersectionProperties(e, intersectionEdge, ride);

                intersectionEdge.setLine(e.getFirst().getLine());
                intersectionEdge.setPrevLine(e.getFirst().getPrevLine());
                intersectionEdge.setNextLine(e.getFirst().getNextLine());
                intersectionEdgeList.add(intersectionEdge);
            }
        }

        for (List<MatchedPoint> intersection : allIntersections) {
            if (intersection.size() < 2) {
                continue;
            }
            IntersectionNode intersectionNode = new IntersectionNode();
            applyIntersectionProperties(intersection, intersectionNode, ride);

            TrafficSignalCluster cluster = intersection.get(1).getInIntersectionCluster();
            intersectionNode.setTrafficSignalCluster(cluster);
            intersectionNode.setEndLine(intersection.getLast().getLine());
            intersectionNode.setStartLine(intersection.getFirst().getLine());

            List<String> names = new ArrayList<>();
            for (MatchedPoint point : intersection) {
                PlanetOsmLine line = point.getLine();
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

    private void applyIntersectionProperties(List<MatchedPoint> intersection, IntersectionBaseClass element, Ride ride) {
        Date endTime = intersection.getLast().getTimestamp();
        Date startTime = intersection.getFirst().getTimestamp();
        element.setEndTime(endTime);
        element.setStartTime(startTime);
        double duration = (double) (endTime.getTime() - startTime.getTime()) / 1000;
        element.setDuration(duration);
        element.setRide(ride);
        Coordinate[] lineString = new Coordinate[intersection.size()];
        List<String> names = new ArrayList<>();
        for (int i = 0; i < intersection.size(); i++) {
            MatchedPoint point = intersection.get(i);
            PlanetOsmLine line = point.getLine();
            if (line != null && line.getName() != null && (names.isEmpty() || !names.getLast().equals(line.getName()))) {
                names.add(line.getName());
            }
            lineString[i] = point.getCoordinate();
        }
        double length = geoService.getLength(List.of(lineString));
        element.setLength(length);
        element.setSpeed(3.6 * length/duration);
        element.setGeom(geometryFactory.createLineString(lineString));
        element.setStartPoint(intersection.getFirst().getGeom());
    }

    private void setWaitingTimes(List<IntersectionNode> intersectionNodeList, List<IntersectionEdge> intersectionEdgeList) {
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
    }


    private void saveMatchedPoints(List<List<MatchedPoint>> sortedPoints) {
        List<MatchedPoint> matchedPointList = new ArrayList<>();
        for (int i = 0; i < sortedPoints.size(); i++) {
            for (int j = 0; j < sortedPoints.get(i).size(); j++) {
                MatchedPoint mP = sortedPoints.get(i).get(j);
                mP.setEdgeId(i);
                mP.setPointInEdgeId(j);
                matchedPointList.add(mP);
            }
        }
        matchedPointRepository.saveAll(matchedPointList);
    }

    private void saveRidePoints(Ride ride) {
        List<RidePoint> ridePointList = new ArrayList<>();
        for (MatchInformation loc : ride.getCoordinates()) {
            RidePoint p = new RidePoint();
            p.setRide(ride);
            p.setTimestamp(new Date(loc.getTimestamp() * 1000));
            Coordinate coord = new Coordinate(loc.getLng(), loc.getLat());
            p.setGeom(geometryFactory.createPoint(coord));
            ridePointList.add(p);
        }
        ridePointRepository.saveAll(ridePointList);
    }

    public void setContainingRegions(List<? extends IntersectionBaseClass> intersections) {
        int number_of_elements = intersections.size();
        if (number_of_elements > 0) {
            Long[] ids = new Long[number_of_elements];
            Double[] lngs = new Double[number_of_elements];
            Double[] lats = new Double[number_of_elements];

            for (int i = 0; i < number_of_elements; i++) {
                IntersectionBaseClass inc = intersections.get(i);
                ids[i] = (long) i;
                lngs[i] = inc.getStartPoint().getX();
                lats[i] = inc.getStartPoint().getY();
            }

            List<Long[]> matches = regionRepository.findContainingRegions(ids, lngs, lats);

            for  (Long[] match : matches) {
                int edgeId = Math.toIntExact(match[0]);
                Long regionId = match[1];
                Region ref = new Region();
                ref.setId(regionId);
                intersections.get(edgeId).getRegions().add(ref);
            }
        }
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

    public Map<String, Object> getIntersectionNodeMetrics(
            Long trafficSignalClusterId, Long count, String region, String streetNames,
            List<WeekDays> weekDay, List<TrafficTimes> trafficTime, List<Integer> year, Pageable pageable) {

        Specification<IntersectionNodeMetrics> spec = Specification
                .where(IntersectionNodeMetricsSpecifications.hasTrafficSignalClusterId(trafficSignalClusterId))
                .and(IntersectionNodeMetricsSpecifications.hasMinCount(count))
                .and(IntersectionNodeMetricsSpecifications.hasName(streetNames))
                .and(IntersectionNodeMetricsSpecifications.hasRegion(region))
                .and(IntersectionNodeMetricsSpecifications.hasWeekDay(weekDay)
                .and(IntersectionNodeMetricsSpecifications.hasTrafficTime(trafficTime)
                .and(IntersectionNodeMetricsSpecifications.hasYear(year))));

        Page<IntersectionNodeMetrics> result = intersectionNodeMetricsRepository.findAll(spec, pageable);

        return GeoService.getFeatureCollection(result);
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

    public Map<String, Object> getIntersectionEdgeMetrics(
            Long count, String region, String name,
            List<WeekDays> weekDay, List<TrafficTimes> trafficTime, List<Integer> year, Pageable pageable) {

        Specification<IntersectionEdgeMetrics> spec = Specification
                .where(IntersectionEdgeMetricsSpecifications.hasMinCount(count))
                .and(IntersectionEdgeMetricsSpecifications.hasName(name))
                .and(IntersectionEdgeMetricsSpecifications.hasRegion(region))
                .and(IntersectionEdgeMetricsSpecifications.hasWeekDay(weekDay))
                .and(IntersectionEdgeMetricsSpecifications.hasTrafficTime(trafficTime))
                .and(IntersectionEdgeMetricsSpecifications.hasYear(year));

        Page<IntersectionEdgeMetrics> result = intersectionEdgeMetricsRepository.findAll(spec, pageable);

        return GeoService.getFeatureCollection(result);
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

    @LogExecutionTime
    public void updateIntersectionMetrics() {
        intersectionNodeMetricsRepository.updateIntersectionNodeMetrics();
        intersectionEdgeMetricsRepository.updateIntersectionEdgeMetrics();
    }
}
