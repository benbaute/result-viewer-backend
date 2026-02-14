package com.simra.konsumgandalf.rides.controllers;

import com.simra.konsumgandalf.common.models.entities.Region;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.common.utils.services.GeoService;
import com.simra.konsumgandalf.rides.services.RideService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/intersections")
public class RideController {
    @Autowired
    private RideService rideService;

	@GetMapping("/{rideId}/points")
	public ResponseEntity<Map<String, Object>> getRidePointsAsGeoJson(@PathVariable Long rideId) {
		return ResponseEntity.ok(GeoService.getFeatureCollection(rideService.getRidePoints(rideId)));
	}

	@GetMapping("/{rideId}/matched_points")
	public ResponseEntity<Map<String, Object>> getMatchedPointsAsGeoJson(@PathVariable Long rideId) {
		return ResponseEntity.ok(GeoService.getFeatureCollection(rideService.getMatchedPoints(rideId)));
	}


    @GetMapping("/{rideId}/intersection_nodes")
    public ResponseEntity<Map<String, Object>> getIntersectionNodesAsGeoJson(@PathVariable Long rideId) {
        return ResponseEntity.ok(GeoService.getFeatureCollection(rideService.getIntersectionNodes(rideId)));
    }

    @GetMapping("/{rideId}/intersection_edges")
    public ResponseEntity<Map<String, Object>> getIntersectionEdgeAsGeoJson(@PathVariable Long rideId) {
        return ResponseEntity.ok(GeoService.getFeatureCollection(rideService.getIntersectionEdge(rideId)));
    }

    @GetMapping("/intersection_nodes")
    public ResponseEntity<Map<String, Object>> getIntersectionNodesAsGeoJson(
            @RequestParam Long trafficSignalClusterId,
            @RequestParam(required = false) Long startOsmId,
            @RequestParam(required = false) Long endOsmId) {
        return ResponseEntity.ok(GeoService.getFeatureCollection(
                rideService.getIntersectionNodes(trafficSignalClusterId, startOsmId, endOsmId)));
    }

    @GetMapping("/intersection_nodes/aggregate/complete")
    public ResponseEntity<Map<String, Object>> getIntersectionNodesAggregateAsGeoJsonComplete(
            @RequestParam(required = false) Long numberOfRides,
            @RequestParam(required = false) WeekDays weekDay,
            @RequestParam(required = false) TrafficTimes trafficTime,
            @RequestParam(required = false) Integer year
    ) {
        return ResponseEntity.ok(GeoService.getFeatureCollection(rideService.getIntersectionNodeMetricsComplete(
                numberOfRides, weekDay, trafficTime, year)));
    }

    @GetMapping("/intersection_nodes/aggregate")
    public ResponseEntity<Map<String, Object>> getIntersectionNodesAggregateAsGeoJsonPageable(
            @RequestParam(required = false) Long trafficSignalClusterId,
            @RequestParam(required = false) Long numberOfRides,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String streetNames,
            @RequestParam(required = false) WeekDays weekDay,
            @RequestParam(required = false) TrafficTimes trafficTime,
            @RequestParam(required = false) Integer year,
            Pageable pageable) {

        return ResponseEntity.ok(rideService.getIntersectionNodeMetricsPageable(
                trafficSignalClusterId, numberOfRides, region, streetNames,
                weekDay, trafficTime, year, pageable));
    }

    @GetMapping("/intersection_nodes/streetNames")
    public List<String> getHighwayNames(
            @RequestParam(required = false) Long trafficSignalClusterId,
            @RequestParam(required = false) Long count,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String streetNames) {
        return rideService.findAllStreetNamesIncludingStringIntersectionNode(trafficSignalClusterId, count, region, streetNames);
    }

    @GetMapping("/intersection_edges")
    public ResponseEntity<Map<String, Object>> getIntersectionEdgesAsGeoJsonPageable(
            @RequestParam(required = false) Long prevOsmId,
            @RequestParam(required = false) Long osmId,
            @RequestParam(required = false) Long nextOsmId) {
        return ResponseEntity.ok(GeoService.getFeatureCollection(
                rideService.getIntersectionEdge(prevOsmId, osmId, nextOsmId)));
    }

    @GetMapping("/intersection_edges/aggregate/complete")
    public ResponseEntity<Map<String, Object>> getIntersectionEdgesAggregateAsGeoJsonComplete(
            @RequestParam(required = false) Long numberOfRides,
            @RequestParam(required = false) WeekDays weekDay,
            @RequestParam(required = false) TrafficTimes trafficTime,
            @RequestParam(required = false) Integer year
    ) {
        return ResponseEntity.ok(GeoService.getFeatureCollection(rideService.getIntersectionEdgeMetricsComplete(
                numberOfRides, weekDay, trafficTime, year)));
    }

    @GetMapping("/intersection_edges/aggregate")
    public ResponseEntity<Map<String, Object>> getIntersectionEdgesAsGeoJsonPageable(
            @RequestParam(required = false) Long numberOfRides,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) WeekDays weekDay,
            @RequestParam(required = false) TrafficTimes trafficTime,
            @RequestParam(required = false) Integer year,
            Pageable pageable) {
        return ResponseEntity.ok(rideService.getIntersectionEdgeMetricsPageable(
                numberOfRides, region, name, weekDay, trafficTime, year, pageable));
    }

    @GetMapping("/intersection_edges/streetNames")
    public List<String> getEdgeHighwayNames(
            @RequestParam(required = false) Long count,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String name) {
        return rideService.findAllStreetNamesIntersectionEdge(count, region, name);
    }

	@GetMapping("/ids")
	public ResponseEntity<List<Long>> getRideIds() {
		List<Long> ids = rideService.getRideIds();
		return ResponseEntity.ok(ids);
	}

    @GetMapping("/rideIds/{osmLineId}")
    public ResponseEntity<List<Long>> findByOsmLineId(@PathVariable Long osmLineId) {
        List<Long> rideIds = rideService.findByOsmLineId(osmLineId);
        return ResponseEntity.ok(rideIds);
    }

    @GetMapping("/regions/complete")
    public ResponseEntity<Map<String, Object>> getIntersectionRegionMetricsComplete(
            @RequestParam(required = false) Long numberOfRides,
            @RequestParam(required = false) WeekDays weekDay,
            @RequestParam(required = false) TrafficTimes trafficTime,
            @RequestParam(required = false) Integer year
    ) {
        return ResponseEntity.ok(GeoService.getFeatureCollection(rideService.getIntersectionRegionMetricsComplete(
                numberOfRides, weekDay, trafficTime, year)));
    }

    @GetMapping("/regions/pageable")
    public ResponseEntity<Map<String, Object>> getIntersectionRegionMetricsPageable(
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Long numberOfRides,
            @RequestParam(required = false) WeekDays weekDay,
            @RequestParam(required = false) TrafficTimes trafficTime,
            @RequestParam(required = false) Integer year,
            Pageable pageable
    ) {
        return ResponseEntity.ok(rideService.getIntersectionRegionMetricsPageable(
                regionId, numberOfRides, weekDay, trafficTime, year, pageable));
    }

    @GetMapping("/regions/polygon")
    public ResponseEntity<Map<String, Object>> getRegionAsGeoJson(
            @RequestParam(required = false) String region
    ) {
        List<Region> regions = rideService.findRegionByName(region);
        return ResponseEntity.ok(GeoService.getFeatureCollection(regions));
    }
}
