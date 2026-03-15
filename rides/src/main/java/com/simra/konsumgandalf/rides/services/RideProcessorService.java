package com.simra.konsumgandalf.rides.services;

import com.simra.konsumgandalf.common.logging.LogExecutionTimeSubTask;
import com.simra.konsumgandalf.common.models.classes.Edge;
import com.simra.konsumgandalf.common.models.classes.MatchInformation;
import com.simra.konsumgandalf.common.models.entities.*;
import com.simra.konsumgandalf.common.repositories.PlanetOsmLineRepository;
import com.simra.konsumgandalf.common.services.OsmService;
import com.simra.konsumgandalf.common.utils.services.GeoService;
import com.simra.konsumgandalf.osmPlanet.repositories.RegionRepository;
import com.simra.konsumgandalf.valhalla.models.TraceResponse;
import com.simra.konsumgandalf.valhalla.models.ValhallaEdge;
import com.simra.konsumgandalf.valhalla.models.ValhallaMatchedPoint;
import com.simra.konsumgandalf.valhalla.services.ValhallaMapMatchingService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RideProcessorService {

	@Autowired
	private RidePersistenceService ridePersistenceService;

	private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

	@Autowired
	private PlanetOsmLineRepository planetOsmLineRepository;

	@Autowired
	private RegionRepository regionRepository;

	@Autowired
	private GeoService geoService;

	@Autowired
	private OsmService osmService;

	@Autowired
	private ValhallaMapMatchingService valhallaMapMatchingService;

	RideProcessorService() {
	}

	@LogExecutionTimeSubTask
	public void processRideEntity(RideEntity rideEntity) {
		Ride ride = new Ride(rideEntity.getPath());
		ride.setCoordinates(rideEntity.getCleanLocations());
		saveRide(ride);
	}

	private void saveRide(Ride ride) {
		ridePersistenceService.saveRide(ride);
		ridePersistenceService.saveRidePointsAndSetRideIds(ride);

		// Sort points by edge osm id
		List<List<MatchedPoint>> sortedPoints = getSortedPointsAndEnrichPoints(ride,
				valhallaMapMatchingService.getTraceAttributes(ride.getCoordinates()));

		List<MatchedPoint> unsortedPoints = new ArrayList<>();
		for (List<MatchedPoint> sortedPoint : sortedPoints) {
			unsortedPoints.addAll(sortedPoint);
		}

		if (unsortedPoints.isEmpty()) {
			return; // No successfully matched points, so no further calculation required
		}

		putIntersectionAndTrafficSignalCluster(unsortedPoints);
		List<List<List<MatchedPoint>>> sortedRideParts = getRidePartsAndPutStops(sortedPoints);
		ridePersistenceService.saveMatchedPoints(unsortedPoints);

		List<IntersectionNode> intersectionNodeList = new ArrayList<>();
		List<IntersectionEdge> intersectionEdgeList = new ArrayList<>();
		for (List<List<MatchedPoint>> sortedRidePart : sortedRideParts) {
			getIntersections(ride, sortedRidePart, intersectionEdgeList, intersectionNodeList);
		}
		setWaitingTimesAndMedianSpeed(intersectionNodeList, intersectionEdgeList);

		setContainingRegions(intersectionEdgeList);
		setContainingRegions(intersectionNodeList);

		ridePersistenceService.saveIntersectionsLists(intersectionNodeList, intersectionEdgeList);
	}

	private List<MatchedPoint> getEnrichedPoints(Ride ride, TraceResponse traceResponse)
			throws IllegalArgumentException {
		List<MatchedPoint> matchedPoints = new ArrayList<>();

		if (traceResponse.getMatchedPoints().size() != traceResponse.getPayloadCoordinates().size()) {
			throw new IllegalArgumentException("Mismatch between matched Points and Ride locations");
		}
		if (traceResponse.getMatchedPoints().isEmpty())
			return matchedPoints;

		MatchInformation prev = traceResponse.getPayloadCoordinates().getFirst();
		Point prevRawGPSLocation = geometryFactory.createPoint(new Coordinate(prev.getLng(), prev.getLat()));
		for (int i = 0; i < traceResponse.getMatchedPoints().size(); i++) {
			MatchedPoint matchedPoint = new MatchedPoint();
			matchedPoint.setRide(ride);
			matchedPoint.setPrevRawGPSLocation(prevRawGPSLocation);

			ValhallaMatchedPoint resultPoint = traceResponse.getMatchedPoints().get(i);
			matchedPoint.setEdgeIndex(resultPoint.getEdgeIndex());
			matchedPoint.setDistanceFromTracePoint(resultPoint.getDistanceFromTracePoint());
			matchedPoint.setMatchingResult(resultPoint.getMatchingResult());
			matchedPoint
				.setGeom(geometryFactory.createPoint(new Coordinate(resultPoint.getLon(), resultPoint.getLat())));

			MatchInformation requestPoint = traceResponse.getPayloadCoordinates().get(i);
			matchedPoint.setTimestamp(new Date(requestPoint.getTimestamp() * 1000));
			matchedPoint.setAccuracy(requestPoint.getAccuracy());
			matchedPoint.setRidePoint(new RidePoint(requestPoint.getRidePointId()));
			matchedPoint.setRawGPSLocation(
					geometryFactory.createPoint(new Coordinate(requestPoint.getLng(), requestPoint.getLat())));
			prevRawGPSLocation = matchedPoint.getRawGPSLocation();

			matchedPoints.add(matchedPoint);
		}
		return matchedPoints;
	}

	private List<Edge> getEnrichedEdges(TraceResponse traceResponse) throws IllegalArgumentException {
		List<Edge> edges = new ArrayList<>();
		List<Long> wayIds = traceResponse.getEdges()
			.stream()
			.map(ValhallaEdge::getId)
			.filter(Objects::nonNull)
			.distinct()
			.toList();

		List<Object[]> resultsTrafficSignalClusters = osmService.findTrafficSignalClustersByOsmLineIds(wayIds);
		Map<Long, List<TrafficSignalCluster>> clusterMap = new HashMap<>();
		for (Object[] row : resultsTrafficSignalClusters) {
			Long osmId = ((Number) row[0]).longValue();
			TrafficSignalCluster cluster = (TrafficSignalCluster) row[1];

			clusterMap.computeIfAbsent(osmId, k -> new ArrayList<>()).add(cluster);
		}

		List<PlanetOsmLine> resultsLines = planetOsmLineRepository.findByIds(wayIds);
		Map<Long, PlanetOsmLine> linesMap = new HashMap<>();
		for (PlanetOsmLine line : resultsLines) {
			linesMap.put(line.getId(), line);
		}

		for (ValhallaEdge valhallaEdge : traceResponse.getEdges()) {
			Edge edge = new Edge();
			edge.setValhallaEdgeId(valhallaEdge.getValhallaEdgeId());

			Long wayId = valhallaEdge.getId();
			if (linesMap.containsKey(wayId)) {
				edge.setOsmId(wayId);
				edge.setOsmLine(linesMap.get(wayId));
			}
			else {
				throw new IllegalArgumentException("Could not find line with osm id: " + wayId);
			}

			edge.setTrafficSignalClusters(clusterMap.containsKey(wayId) ? clusterMap.get(wayId) : new ArrayList<>());
			edges.add(edge);
		}

		return edges;
	}

	/**
	 * Sorts the points into lists with the same valhalla edge id. Enriches the points
	 * with the following information based on the edge it belongs to: osmLine: osmLine of
	 * edge prevOsmLine: osmLine of previous edge nextOsmLine: osmLine of next edge
	 * valhallaEdgeId: valhallaEdgeId of edge prevValhallaEdgeId: valhallaEdgeId of
	 * previous edge nextValhallaEdgeId: valhallaEdgeId of next edge
	 * trafficSignalClusters: intersection of edge with traffic signal clusters or
	 * trafficSignalClusters from previous and next edge if no corresponding edge If there
	 * is no edge the line and id information is null If there is no edge for the first or
	 * last point group, those point groups are discarded.
	 * @param ride - The current ride
	 * @param traceResponse - The points and edges from valhalla
	 * @return - The sorted points
	 */
	private List<List<MatchedPoint>> getSortedPointsAndEnrichPoints(Ride ride, TraceResponse traceResponse)
			throws IllegalArgumentException {
		List<List<MatchedPoint>> sortedPoints = getSortedPoints(getEnrichedPoints(ride, traceResponse),
				getEnrichedEdges(traceResponse));

		if (sortedPoints.isEmpty())
			return sortedPoints;
		discardUnMatchedPointsAt(sortedPoints, true);
		discardUnMatchedPointsAt(sortedPoints, false);

		// Put previous and next osm id / valhalla edge id
		for (int i = 1; i < sortedPoints.size(); i++) {
			MatchedPoint prev = sortedPoints.get(i - 1).getFirst();
			for (MatchedPoint p : sortedPoints.get(i)) {
				p.setPrevOsmLine(prev.getOsmLine());
				p.setPrevValhallaEdgeId(prev.getValhallaEdgeId());
			}
		}
		for (int i = 0; i < sortedPoints.size() - 1; i++) {
			MatchedPoint next = sortedPoints.get(i + 1).getFirst();
			for (MatchedPoint p : sortedPoints.get(i)) {
				p.setNextOsmLine(next.getOsmLine());
				p.setNextValhallaEdgeId(next.getValhallaEdgeId());
			}
		}

		// And traffic signal clusters for points without edge
		for (int i = 1; i < sortedPoints.size() - 1; i++) {
			List<MatchedPoint> current = sortedPoints.get(i);
			if (current.getFirst().getValhallaEdgeId() == null) {
				List<TrafficSignalCluster> prev = sortedPoints.get(i - 1).getFirst().getTrafficSignalClusters();
				List<TrafficSignalCluster> next = sortedPoints.get(i + 1).getFirst().getTrafficSignalClusters();
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
	 * Sorts the points into lists with the same valhalla edge id. Enriches the points
	 * with the following information based on the edge it belongs to: osmLine: osmLine of
	 * edge valhallaEdgeId: valhallaEdgeId of edge trafficSignalClusters: intersection of
	 * edge with traffic signal clusters If there is no edge the information is null
	 * Points with below a certain threshold are ignored to avoid edges of distance 0
	 * @param points - Enriched points from valhalla
	 * @param edges - Enriched edges from valhalla
	 * @return - The sorted points
	 */
	private List<List<MatchedPoint>> getSortedPoints(List<MatchedPoint> points, List<Edge> edges) {
		List<List<MatchedPoint>> sortedPoints = new ArrayList<>();
		if (points.isEmpty())
			return sortedPoints;

		List<MatchedPoint> currentPoints = new ArrayList<>();
		currentPoints.add(points.getFirst());

		for (MatchedPoint matchedPoint : points) {
			// Ignore points on small distance
			if (geoService.distance(currentPoints.getLast().getGeom(), matchedPoint.getGeom()) < 0.1)
				continue;

			if (matchedPoint.getEdgeIndex() != null) {
				Edge edge = edges.get(matchedPoint.getEdgeIndex());

				// Put edge information on point
				matchedPoint.setValhallaEdgeId(edge.getValhallaEdgeId());
				matchedPoint.setOsmLine(edge.getOsmLine());
				matchedPoint.setTrafficSignalClusters(edge.getTrafficSignalClusters());
			}

			if (!Objects.equals(currentPoints.getLast().getValhallaEdgeId(), matchedPoint.getValhallaEdgeId())) {
				// If different way id, a new edge is created, and the current edge is
				// saved
				sortedPoints.add(currentPoints);
				currentPoints = new ArrayList<>();
			}
			currentPoints.add(matchedPoint);
		}
		sortedPoints.add(currentPoints);
		return sortedPoints;
	}

	private void discardUnMatchedPointsAt(List<List<MatchedPoint>> points, boolean front) {
		while (!points.isEmpty() && front ? points.getFirst().getFirst().getValhallaEdgeId() == null
				: points.getLast().getLast().getValhallaEdgeId() == null) {
			if (front)
				points.removeFirst();
			else
				points.removeLast();
		}
	}

	/**
	 * Adds whether a point belongs to an intersection, and which intersection it belongs
	 * to. The point before and after are also added to the intersection
	 * @param points - The enriched points from valhalla, each point must have a list of
	 * TrafficSignalCluster
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
					MatchedPoint prevPoint = points.get(i - 1);
					prevPoint.setInIntersection(true);
				}
				inIntersection = true;
			}
			else {
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
				// If in intersection, but not belonging to a cluster, move it to
				// belonging cluster
				if (i < points.size() - 1) {
					// Point belongs to cluster following it, in case it exists
					MatchedPoint nextPoint = points.get(i + 1);
					point.setInIntersectionCluster(nextPoint.getInIntersectionCluster());
				}
				if (i > 0 && point.getInIntersectionCluster() == null) {
					// Else point belongs to cluster before it
					MatchedPoint prevPoint = points.get(i - 1);
					point.setInIntersectionCluster(prevPoint.getInIntersectionCluster());
				}
			}
		}
	}

	/**
	 * Splits a ride into multiple parts. It adds a stop if the distance behaves
	 * unexpectantly. The edge containing the stop is not added to any ride part.
	 * @param sortedPoints - The enriched list of lists of points from valhalla, each
	 * point must have a Coordinate. The inner list contains all points with the same edge
	 * index (if in order) = edge The outer list contains all edges of the ride.
	 * @return - The ride parts.
	 */
	private List<List<List<MatchedPoint>>> getRidePartsAndPutStops(List<List<MatchedPoint>> sortedPoints) {
		List<List<List<MatchedPoint>>> sortedRideParts = new ArrayList<>();
		if (sortedPoints.isEmpty()) {
			return sortedRideParts;
		}
		List<List<MatchedPoint>> currentRouteParts = new ArrayList<>();

		List<MatchedPoint> pointsInRage = new ArrayList<>();
		MatchedPoint previousPoint = sortedPoints.getFirst().getFirst();
		int sizeLoopCheck = 5;
		int stops = 0;

		for (List<MatchedPoint> edge : sortedPoints) {
			boolean foundStop = false;
			for (int i = 0; i < edge.size(); i++) {
				MatchedPoint currentPoint = edge.get(i);
				currentPoint.setStops(stops);

				List<Double> distances = new ArrayList<>();
				for (int j = 1; i + j < edge.size() && distances.size() < sizeLoopCheck; j++) {
					distances.add(geoService.distance(currentPoint.getGeom(), edge.get(i + j).getGeom()));
				}
				for (int j = 0; j < distances.size() - 1; j++) {
					if (distances.get(j) - distances.get(j + 1) > 2) {
						// Add stop, if distance did not increase on current edge
						// (considering threshold of 2 meters)
						// This can lead to false stop detections on winding roads (or on
						// roads without osm detection)
						foundStop = true;
						break;
					}
				}

				if (geoService.distance(currentPoint.getRawGPSLocation(), currentPoint.getPrevRawGPSLocation()) > 60) {
					// Add stop if distance between raw GPS point is unexpectantly large
					// Difficult to select proper value to avoid cutting at short tunnels
					// With a low threshold of shorter than 100 meters this may cut even
					// objectively fine rides,
					// but this avoids bad GPS rides
					foundStop = true;
				}
				if (geoService.distance(currentPoint.getGeom(), previousPoint.getGeom()) > 100) {
					// Add stop if distance to previous point is exceeding 100 meters
					// This should only happen due to bad GPS tracking/matching
					foundStop = true;
				}
				previousPoint = currentPoint;

				while (!pointsInRage.isEmpty()
						&& geoService.distance(currentPoint.getGeom(), pointsInRage.getFirst().getGeom()) > 50) {
					pointsInRage.removeFirst(); // remove points with larger distance
				}
				if (!pointsInRage.isEmpty()) {
					double maxDistanceFromTracePoint = 0;
					for (MatchedPoint point : pointsInRage) {
						Double distance_from_trace_point = point.getDistanceFromTracePoint();
						if (distance_from_trace_point != null
								&& distance_from_trace_point > maxDistanceFromTracePoint) {
							maxDistanceFromTracePoint = distance_from_trace_point;
						}
					}
					long diffToPointInRage = ((currentPoint.getTimestamp()).getTime()
							- (pointsInRage.getFirst().getTimestamp()).getTime()) / 1000;
					boolean inIntersection = currentPoint.getInIntersection();
					if ((inIntersection && diffToPointInRage > 60 * 4)
							|| (!inIntersection && diffToPointInRage > 60 * 2)
							|| (maxDistanceFromTracePoint > 15 && diffToPointInRage > 60 * 2)) {
						// Add stop if not moved more than 50 meters
						// ... in the last 4 minutes and in intersection
						// ... in the last 2 minutes and not in intersection
						// ... in the last 2 minutes and a large distance from trace point
						// (in range) indicating a short break
						foundStop = true;
					}
				}
				pointsInRage.add(currentPoint);
			}
			if (!foundStop) {
				// Adds, only parts without stops
				currentRouteParts.add(edge);
			}
			else {
				stops++;
				this.removeOnStartEndCondition(currentRouteParts);
				// If a stop occurred, all previous edges are placed into one list.
				if (!currentRouteParts.isEmpty()) {
					sortedRideParts.add(currentRouteParts);
					currentRouteParts = new ArrayList<>();
				}
			}
		}
		this.removeOnStartEndCondition(currentRouteParts);
		if (!currentRouteParts.isEmpty()) {
			sortedRideParts.add(currentRouteParts);
		}
		return sortedRideParts;
	}

	private void removeOnStartEndCondition(List<List<MatchedPoint>> routeParts) {
		trimRouteParts(routeParts, true);
		if (!routeParts.isEmpty())
			routeParts.removeFirst();
		trimRouteParts(routeParts, true);

		trimRouteParts(routeParts, false);
		if (!routeParts.isEmpty())
			routeParts.removeLast();
		trimRouteParts(routeParts, false);
	}

	private void trimRouteParts(List<List<MatchedPoint>> routeParts, boolean front) {
		while (!routeParts.isEmpty()) {
			boolean trim = checkEndStartCondition(front ? routeParts.getFirst() : routeParts.getLast());
			if (routeParts.size() > 1) {
				if (front) {
					if (geoService.distance(routeParts.getFirst().getLast().getGeom(),
							routeParts.get(1).getFirst().getGeom()) > 30) {
						trim = true;
					}
				}
				else {
					if (geoService.distance(routeParts.getLast().getFirst().getGeom(),
							routeParts.get(routeParts.size() - 2).getLast().getGeom()) > 30) {
						trim = true;
					}
				}
			}
			if (!trim)
				return;
			if (front)
				routeParts.removeFirst();
			else
				routeParts.removeLast();
		}
	}

	private boolean checkEndStartCondition(List<MatchedPoint> edge) {
		PlanetOsmLine line = edge.getFirst().getOsmLine();
		if (line != null && line.getHighway().equals("service")) {
			// If edge is a service way, remove service way (as this is usually unintended
			// ride)
			return true;
		}

		Point previous = edge.getFirst().getGeom();
		Double minDistance = edge.getFirst().getDistanceFromTracePoint();
		for (MatchedPoint currentPoint : edge) {
			if (currentPoint.getMatchingResult().equals("unmatched")) {
				// Unmatched points imply a large distance from osm line.
				// At start or end this is usually inside a house
				return true;
			}

			if (geoService.distance(previous, currentPoint.getGeom()) > 30) {
				// Start or end should not contain any jumps
				return true;
			}
			previous = currentPoint.getGeom();

			Double distanceFromTracePoint = currentPoint.getDistanceFromTracePoint();
			if (distanceFromTracePoint == null)
				continue;
			if (distanceFromTracePoint > 15) {
				// If the edge contains a point with a large distance from the trace
				// point, the current position
				// is usually wrongly mapped as the position is most likely not on the
				// path but inside a house
				return true;
			}

			if (distanceFromTracePoint < minDistance)
				minDistance = distanceFromTracePoint;
		}

		for (MatchedPoint currentPoint : edge) {
			Double distanceFromTracePoint = currentPoint.getDistanceFromTracePoint();
			if (distanceFromTracePoint != null && distanceFromTracePoint - minDistance > 5) {
				// If there is a large deviation to the min distance
				// is usually wrongly mapped as the position is most likely not on the
				// path but inside a house
				return true;
			}
		}

		return false;
	}

	private <T extends IntersectionBase> IntersectionBase finalizeElement(T element, List<T> list,
			IntersectionBase previous) {
		if (element.getMatchedPoints().isEmpty()) {
			return previous;
		}
		element.setPrevIntersection(previous);
		list.add(element);
		return element;
	}

	private void fillIntersectionLists(List<List<MatchedPoint>> sortedPoints, List<IntersectionEdge> edgeList,
			List<IntersectionNode> nodeList) {

		if (sortedPoints.isEmpty()) {
			return;
		}

		IntersectionBase previous = null;
		IntersectionNode currentNode = new IntersectionNode();
		Long currentClusterId = null;
		for (List<MatchedPoint> sortedEdge : sortedPoints) {
			// Each sorted edge has exactly one osm id, as long as no points are in an
			// intersection
			// all points remain on the same edge
			IntersectionEdge currentEdge = new IntersectionEdge();
			for (MatchedPoint point : sortedEdge) {
				if (point.getInIntersection()) {
					Long clusterId = point.getInIntersectionCluster().getId();
					if (!clusterId.equals(currentClusterId)) {
						previous = finalizeElement(currentNode, nodeList, previous);
						currentNode = new IntersectionNode();

						currentClusterId = clusterId;
					}
					previous = finalizeElement(currentEdge, edgeList, previous);
					currentEdge = new IntersectionEdge();

					currentNode.getMatchedPoints().add(point);
				}
				else {
					previous = finalizeElement(currentNode, nodeList, previous);
					currentNode = new IntersectionNode();

					currentEdge.getMatchedPoints().add(point);
				}
			}
			previous = finalizeElement(currentEdge, edgeList, previous);
		}

		if (!nodeList.isEmpty() && sortedPoints.getFirst().getFirst().getInIntersection()) {
			// Skips first node if ride starts with it as that is likely incomplete.
			// The last node is skipped as well if the ride ends with it, as it is not
			// finalized
			nodeList.removeFirst();
		}
	}

	private void getIntersections(Ride ride, List<List<MatchedPoint>> sortedPoints, List<IntersectionEdge> allEdges,
			List<IntersectionNode> allNodes) {

		List<IntersectionNode> newNodes = new ArrayList<>();
		List<IntersectionEdge> newEdges = new ArrayList<>();
		fillIntersectionLists(sortedPoints, newEdges, newNodes);

		if (!newEdges.isEmpty() && newEdges.getFirst().getMatchedPoints().size() == 1) {
			// The only edge which can consists of one points is the first one,
			// as all other edges get connected to the previous element
			newEdges.removeFirst();
		}
		for (IntersectionEdge edge : newEdges) {
			MatchedPoint point = edge.getMatchedPoints().getFirst();
			edge.setOsmLine(point.getOsmLine());
			edge.setPrevOsmLine(point.getPrevOsmLine());
			edge.setNextOsmLine(point.getNextOsmLine());

			edge.setValhallaEdgeId(point.getValhallaEdgeId());
			edge.setPrevValhallaEdgeId(point.getPrevValhallaEdgeId());
			edge.setNextValhallaEdgeId(point.getNextValhallaEdgeId());

			addLastPointOfPreviousElement(edge); // This prepends a point with a (usually)
													// different osmId
			applyIntersectionProperties(edge, ride);
		}

		for (IntersectionNode node : newNodes) {
			node.setEndOsmLine(node.getMatchedPoints().getLast().getOsmLine());
			node.setStartOsmLine(node.getMatchedPoints().getFirst().getOsmLine());

			node.setTrafficSignalCluster(node.getMatchedPoints().getFirst().getInIntersectionCluster());
			node.setEndValhallaEdgeId(node.getMatchedPoints().getLast().getValhallaEdgeId());
			node.setStartValhallaEdgeId(node.getMatchedPoints().getFirst().getValhallaEdgeId());

			setStreetNamesOnIntersectionNode(node);

			addLastPointOfPreviousElement(node);
			applyIntersectionProperties(node, ride);
		}

		allNodes.addAll(newNodes);
		allEdges.addAll(newEdges);
	}

	private void setStreetNamesOnIntersectionNode(IntersectionNode intersectionNode) {
		List<String> names = new ArrayList<>();
		for (MatchedPoint point : intersectionNode.getMatchedPoints()) {
			PlanetOsmLine line = point.getOsmLine();
			if (line != null && line.getName() != null
					&& (names.isEmpty() || !names.getLast().equals(line.getName()))) {
				names.add(line.getName());
			}
		}

		TrafficSignalCluster cluster = intersectionNode.getMatchedPoints().getFirst().getInIntersectionCluster();
		if (names.isEmpty()) {
			List<String> namesCluster = cluster.getOsmLinesName();
			for (int i = 0; namesCluster != null && i < namesCluster.size() && i < 2; i++) {
				names.add(namesCluster.get(i));
			}
		}
		intersectionNode.setStreetNames(String.join(";", names));
	}

	private void addLastPointOfPreviousElement(IntersectionBase element) {
		// Add last point of previous element to remove gap
		if (element.getPrevIntersection() != null) {
			element.getMatchedPoints().addFirst(element.getPrevIntersection().getMatchedPoints().getLast());
		}
	}

	private void applyIntersectionProperties(IntersectionBase element, Ride ride) {
		element.setRide(ride);

		List<MatchedPoint> points = element.getMatchedPoints();
		element.setStartPoint(points.getFirst().getGeom());
		element.setEndTime(points.getLast().getTimestamp());
		element.setStartTime(points.getFirst().getTimestamp());
		element.setDuration((double) (element.getEndTime().getTime() - element.getStartTime().getTime()) / 1000);

		Coordinate[] lineString = points.stream().map(p -> p.getGeom().getCoordinate()).toArray(Coordinate[]::new);
		element.setGeom(geometryFactory.createLineString(lineString));
		element.setLength(geoService.getLength(List.of(lineString)));
		element.setSpeed(3.6 * element.getLength() / element.getDuration());
	}

	private void setWaitingTimesAndMedianSpeed(List<? extends IntersectionBase> elements, double medianSpeed) {
		for (IntersectionBase base : elements) {
			base.setMedianSpeed(medianSpeed * 3.6);
			base.calculateAndSetWaitingTime(medianSpeed);
		}
	}

	private void setWaitingTimesAndMedianSpeed(List<IntersectionNode> intersectionNodeList,
			List<IntersectionEdge> intersectionEdgeList) {
		List<Double> speeds = intersectionEdgeList.stream().map(e -> e.getLength() / e.getDuration()).sorted().toList();
		if (speeds.isEmpty())
			return;
		double medianSpeed = speeds.get(speeds.size() / 2);
		setWaitingTimesAndMedianSpeed(intersectionNodeList, medianSpeed);
		setWaitingTimesAndMedianSpeed(intersectionEdgeList, medianSpeed);
	}

	public void setContainingRegions(List<? extends IntersectionBase> intersections) {
		int number_of_elements = intersections.size();
		if (number_of_elements > 0) {
			Long[] ids = new Long[number_of_elements];
			Double[] lngs = new Double[number_of_elements];
			Double[] lats = new Double[number_of_elements];

			for (int i = 0; i < number_of_elements; i++) {
				IntersectionBase inc = intersections.get(i);
				ids[i] = (long) i;
				lngs[i] = inc.getStartPoint().getX();
				lats[i] = inc.getStartPoint().getY();
			}

			List<Long[]> matches = regionRepository.findContainingRegions(ids, lngs, lats);

			for (Long[] match : matches) {
				int edgeId = Math.toIntExact(match[0]);
				Long regionId = match[1];
				Region ref = new Region();
				ref.setId(regionId);
				intersections.get(edgeId).getRegions().add(ref);
			}
		}
	}

}
