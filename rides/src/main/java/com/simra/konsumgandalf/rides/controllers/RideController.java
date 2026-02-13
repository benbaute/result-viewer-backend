package com.simra.konsumgandalf.rides.controllers;

import com.simra.konsumgandalf.common.models.dtos.IntersectionEdgeAggregate;
import com.simra.konsumgandalf.common.models.dtos.IntersectionNodeAggregate;
import com.simra.konsumgandalf.common.models.dtos.RegionAggregate;
import com.simra.konsumgandalf.common.models.entities.Region;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.common.utils.services.GeoService;
import com.simra.konsumgandalf.rides.services.RideService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    @GetMapping("/intersection_nodes/aggregate")
    public ResponseEntity<Map<String, Object>> getIntersectionNodesAggregateAsGeoJson(
            @RequestParam(required = false) Long trafficSignalClusterId,
            @RequestParam(required = false) Long numberOfRides,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String streetNames,
            @RequestParam(required = false) List<WeekDays> weekDay,
            @RequestParam(required = false) List<TrafficTimes> trafficTime,
            @RequestParam(required = false) List<Integer> year,
            @PageableDefault(size = 10000) Pageable pageable) {

        // List<IntersectionNodeAggregate> intersectionNodes = rideService.aggregateNodes(trafficSignalClusterId, count, region, streetNames);
        // return ResponseEntity.ok(GeoService.getFeatureCollection(intersectionNodes));

        return ResponseEntity.ok(rideService.getIntersectionNodeMetrics(
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
    public ResponseEntity<Map<String, Object>> getIntersectionEdgesAsGeoJson(
            @RequestParam(required = false) Long prevOsmId,
            @RequestParam(required = false) Long osmId,
            @RequestParam(required = false) Long nextOsmId) {
        return ResponseEntity.ok(GeoService.getFeatureCollection(
                rideService.getIntersectionEdge(prevOsmId, osmId, nextOsmId)));
    }

    @GetMapping("/intersection_edges/aggregate")
    public ResponseEntity<Map<String, Object>> getIntersectionEdgesAsGeoJson(
            @RequestParam(required = false) Long numberOfRides,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) List<WeekDays> weekDay,
            @RequestParam(required = false) List<TrafficTimes> trafficTime,
            @RequestParam(required = false) List<Integer> year,
            Pageable pageable) { // TODO: fix limit of 1000


        // List<IntersectionEdgeAggregate> intersectionEdges = rideService.aggregateEdges(count, region, name);
        // return ResponseEntity.ok(GeoService.getFeatureCollection(intersectionEdges));

        return ResponseEntity.ok(rideService.getIntersectionEdgeMetrics(
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

    @GetMapping("/regions/aggregate")
    public ResponseEntity<Map<String, Object>> getAggregateIntersectionDataPerRegion(
            @RequestParam(required = false) String region
    ) {
        List<RegionAggregate> regions = rideService.aggregateIntersectionDataPerRegion(region);
        return ResponseEntity.ok(GeoService.getFeatureCollection(regions));
    }

    @GetMapping("/regions/polygon")
    public ResponseEntity<Map<String, Object>> getRegionAsGeoJson(
            @RequestParam(required = false) String region
    ) {
        List<Region> regions = rideService.findRegionByName(region);
        return ResponseEntity.ok(GeoService.getFeatureCollection(regions));
    }
}
