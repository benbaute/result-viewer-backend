package com.simra.konsumgandalf.rides.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simra.konsumgandalf.common.models.entities.*;
import com.simra.konsumgandalf.common.repositories.PlanetOsmLineRepository;
import com.simra.konsumgandalf.common.utils.services.GeoService;
import com.simra.konsumgandalf.rides.records.EdgeSpeedStats;
import com.simra.konsumgandalf.rides.records.IntersectionDelayGroup;
import com.simra.konsumgandalf.rides.services.RideEntityService;
import com.simra.konsumgandalf.rides.services.RideService;
import org.geolatte.geom.jts.JTS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.proj4j.*;
import org.locationtech.jts.geom.LineString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/ride")
public class RideController {
    @Autowired
    private PlanetOsmLineRepository planetOsmLineRepository;

    @Autowired
    private RideService rideService;

    @Autowired
    private GeoService geoService;

	@GetMapping("/{rideId}/points")
	public ResponseEntity<Map<String, Object>> getRidePointsAsGeoJson(@PathVariable Long rideId) {
		List<RidePoint> points = rideService.getRidePoints(rideId);

		Map<String, Object> geoJson = Map.of("type", "FeatureCollection", "features",
				points.stream()
					.map(p -> Map.of("type", "Feature", "geometry",
							Map.of("type", "Point", "coordinates", List.of(p.getGeom().getX(), p.getGeom().getY())),
							"properties", Map.of("timestamp", p.getTimestamp(),
                                    "path", p.getRide().getPath(),
                                    "ride_id", rideId)))
					.toList());

		return ResponseEntity.ok(geoJson);
	}

	@GetMapping("/{rideId}/matched_points")
	public ResponseEntity<Map<String, Object>> getMatchedPointsAsGeoJson(@PathVariable Long rideId) {
		List<MatchedPoint> points = rideService.getMatchedPoints(rideId);
		Map<String, Object> geoJson = Map.of("type", "FeatureCollection", "features",
				points.stream()
					.map(p -> Map.of("type", "Feature", "geometry",
							Map.of("type", "Point", "coordinates", List.of(p.getGeom().getX(), p.getGeom().getY())),
                                    "properties", Map.of("timestamp", p.getTimestamp(),
                                            "edge_id", p.getEdgeId(), "point_in_edge_id", p.getPointInEdgeId(),
                                    "way_id", p.getLine() != null ? p.getLine().getId() : "null",
                                    "inIntersection", p.getInIntersection(),
                                    "ride_id", rideId)))
					.toList());

		return ResponseEntity.ok(geoJson);
	}

	@GetMapping("/{rideId}/edges")
	public ResponseEntity<Map<String, Object>> getEdgesAsGeoJson(@PathVariable Long rideId) {
		List<Edge> edges = rideService.getEdges(rideId);
		Map<String, Object> geoJson = Map.of("type", "FeatureCollection", "features", edges.stream().map(e -> {
            List<List<Double>> coordinates = geoService.getLineCoordinates(e.getLine()).stream().map(
                    c -> List.of(c.getX(), c.getY())).toList();
			return Map.of("type", "Feature", "geometry", Map.of("type", "LineString", "coordinates", coordinates),
					"properties",
					Map.of( "length", e.getLength(), "way_id", e.getLine().getId(),
                            "direction", e.getDirection() != null ? e.getDirection() : "null", "speed", e.getSpeed(),
                            "startTime", e.getStartTime(),
                            "ride_id", rideId));
		}).toList());

		return ResponseEntity.ok(geoJson);
	}

    @GetMapping("/{rideId}/intersection_delays")
    public ResponseEntity<Map<String, Object>> getIntersectionDelaysAsGeoJson(@PathVariable Long rideId) {
        List<IntersectionDelay> intersectionDelays = rideService.getIntersectionDelays(rideId);
        Map<String, Object> geoJson = Map.of("type", "FeatureCollection", "features", intersectionDelays.stream().map(i -> {
            return Map.of("type", "Feature", "geometry", i.getGeom(),
                    "properties",
                    Map.of( "start_osm_id", i.getStartLine() != null ? i.getStartLine().getId() : "null",
                            "end_osm_id", i.getEndLine() != null ? i.getEndLine().getId() : "null",
                            "start_time", i.getStartTime(),
                            "end_time", i.getEndTime(),
                            "duration", i.getDuration(),
                            "length", i.getLength(),
                            "speed", i.getSpeed(),
                            "ride_id", rideId));
        }).toList());

        return ResponseEntity.ok(geoJson);
    }

    @GetMapping("/intersection_delays")
    public ResponseEntity<Map<String, Object>> getIntersectionDelaysAsGeoJson() {
        ObjectMapper mapper = new ObjectMapper();
        List<IntersectionDelayGroup> intersectionDelays = rideService.aggregateDelays();
        Map<String, Object> geoJson = Map.of("type", "FeatureCollection", "features", intersectionDelays.stream().map(i -> {
            try {
                return Map.of("type", "Feature", "geometry", mapper.readValue(i.getExampleGeom(), Object.class),
                        "properties",
                        Map.of( "start_osm_id", i.getStartLineId() != null ? i.getStartLineId() : "null",
                                "end_osm_id", i.getEndLineId() != null ? i.getEndLineId() : "null",
                                "count", i.getCount(),
                                "avg_length", i.getAvgLength(),
                                "avg_duration", i.getAvgDuration(),
                                "max_duration", i.getMaxDuration(),
                                "avg_speed", i.getAvgSpeed()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).toList());

        return ResponseEntity.ok(geoJson);
    }

	@GetMapping("/ids")
	public ResponseEntity<List<Long>> getRideIds() {
		List<Long> ids = rideService.getRideIds();
		return ResponseEntity.ok(ids);
	}

    @GetMapping("/edge-speeds")
    public ResponseEntity<Map<String, Object>> getAverageSpeeds() {
        List<EdgeSpeedStats> edgeSpeeds = rideService.getAverageSpeeds();
        Map<String, Object> geoJson = Map.of("type", "FeatureCollection", "features", edgeSpeeds.stream().map(e -> {

            Optional<PlanetOsmLine> pLine = planetOsmLineRepository.findById(e.osmId());
            List<List<Double>> coordinates = List.of();
            if (pLine.isPresent()) {
                coordinates = geoService.getLineCoordinates(pLine.get()).stream().map(
                        c -> List.of(c.getX(), c.getY())).toList();
            }

            return Map.of("type", "Feature", "geometry", Map.of("type", "LineString", "coordinates", coordinates),
                    "properties",
                    Map.of( "osm_id", e.osmId(), "avg_speed", e.avgSpeed(), "count", e.count()));
        }).toList());
        return ResponseEntity.ok(geoJson);
    }

    @GetMapping("/rideIds/{osmLineId}")
    public ResponseEntity<List<Long>> findByOsmLineId(@PathVariable Long osmLineId) {
        List<Long> rideIds = rideService.findByOsmLineId(osmLineId);
        return ResponseEntity.ok(rideIds);
    }
}
