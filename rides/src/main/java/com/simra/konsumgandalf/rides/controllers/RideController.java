package com.simra.konsumgandalf.rides.controllers;

import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.common.utils.services.GeoService;
import com.simra.konsumgandalf.rides.services.RideService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
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

	@GetMapping("/{intersectionBaseId}/intersection_base/properties")
	public ResponseEntity<Map<String, Object>> getIntersectionBaseAsGeoJson(@PathVariable Long intersectionBaseId) {
		return rideService.getIntersectionBase(intersectionBaseId)
			.map(base -> ResponseEntity.ok(base.getProperties()))
			.orElseGet(() -> ResponseEntity.ok(null));
	}

	@GetMapping("/intersection_base")
	public ResponseEntity<Map<String, Object>> getIntersectionBaseAsGeoJson(@RequestParam Long id,
			@RequestParam(required = false) WeekDays weekDay, @RequestParam(required = false) TrafficTimes trafficTime,
			@RequestParam(required = false) Integer year, @RequestParam(required = false) Long startDate,
			@RequestParam(required = false) Long endDate) {
		return ResponseEntity
			.ok(GeoService.getFeatureCollection(rideService.getIntersectionBase(id, trafficTime, weekDay, year,
					startDate != null ? new Date(startDate) : null, endDate != null ? new Date(endDate) : null)));
	}

	@GetMapping("/intersection_base/properties")
	public ResponseEntity<List<Map<String, Object>>> getIntersectionBaseProperties(@RequestParam Long id,
			@RequestParam(required = false) WeekDays weekDay, @RequestParam(required = false) TrafficTimes trafficTime,
			@RequestParam(required = false) Integer year, @RequestParam(required = false) Long startDate,
			@RequestParam(required = false) Long endDate) {
		return ResponseEntity
			.ok(GeoService.getPropertiesCollection(rideService.getIntersectionBase(id, trafficTime, weekDay, year,
					startDate != null ? new Date(startDate) : null, endDate != null ? new Date(endDate) : null)));
	}

	@GetMapping("/intersection_base/matched_points")
	public ResponseEntity<Map<String, Object>> getMatchedPointsByBaseIdAsGeoJson(@RequestParam Long id) {
		return ResponseEntity.ok(GeoService.getFeatureCollection(rideService.getMatchedPointsByBaseId(id)));
	}

	@GetMapping("/intersection_base/ride_points")
	public ResponseEntity<Map<String, Object>> getRidePointsByBaseIdAsGeoJson(@RequestParam Long id) {
		return ResponseEntity.ok(GeoService.getFeatureCollection(rideService.getRidePointsByBaseId(id)));
	}

	@GetMapping("/intersection_nodes/trafficSignalClusterId/properties")
	public ResponseEntity<List<Map<String, Object>>> getIntersectionNodePropertiesByTrafficSignalClusterId(
			@RequestParam Long trafficSignalClusterId, @RequestParam WeekDays weekDay,
			@RequestParam TrafficTimes trafficTime, @RequestParam Integer year) {
		return ResponseEntity.ok(GeoService.getPropertiesCollection(
				rideService.getIntersectionNodesByClusterId(trafficSignalClusterId, trafficTime, weekDay, year)));
	}

	@GetMapping("/intersection_nodes/regionId/properties")
	public ResponseEntity<List<Map<String, Object>>> getIntersectionNodePropertiesByRegionId(
			@RequestParam Long regionId, @RequestParam WeekDays weekDay, @RequestParam TrafficTimes trafficTime,
			@RequestParam Integer year) {
		return ResponseEntity.ok(GeoService
			.getPropertiesCollection(rideService.getIntersectionNodesByRegionId(regionId, trafficTime, weekDay, year)));
	}

	@GetMapping("/intersection_nodes/aggregate/complete")
	public ResponseEntity<Map<String, Object>> getIntersectionNodesAggregateAsGeoJsonComplete(
			@RequestParam(required = false) Long numberOfRides, @RequestParam(required = false) WeekDays weekDay,
			@RequestParam(required = false) TrafficTimes trafficTime, @RequestParam(required = false) Integer year) {
		return ResponseEntity.ok(GeoService.getFeatureCollection(
				rideService.getIntersectionNodeMetricsComplete(numberOfRides, weekDay, trafficTime, year)));
	}

	@GetMapping("/intersection_nodes/aggregate")
	public ResponseEntity<Map<String, Object>> getIntersectionNodesAggregateAsGeoJsonPageable(
			@RequestParam(required = false) Long trafficSignalClusterId,
			@RequestParam(required = false) Long numberOfRides, @RequestParam(required = false) String region,
			@RequestParam(required = false) String streetNames, @RequestParam(required = false) WeekDays weekDay,
			@RequestParam(required = false) TrafficTimes trafficTime, @RequestParam(required = false) Integer year,
			Pageable pageable) {

		return ResponseEntity.ok(rideService.getIntersectionNodeMetricsPageable(trafficSignalClusterId, numberOfRides,
				region, streetNames, weekDay, trafficTime, year, pageable));
	}

	@GetMapping("/intersection_nodes/streetNames")
	public List<String> getHighwayNames(@RequestParam(required = false) Long trafficSignalClusterId,
			@RequestParam(required = false) Long count, @RequestParam(required = false) String region,
			@RequestParam(required = false) String streetNames) {
		return rideService.findAllStreetNamesIncludingStringIntersectionNode(trafficSignalClusterId, count, region,
				streetNames);
	}

	@GetMapping("/intersection_edges/osmId/properties")
	public ResponseEntity<List<Map<String, Object>>> getIntersectionEdgePropertiesByOsmId(@RequestParam Long osmId,
			@RequestParam WeekDays weekDay, @RequestParam TrafficTimes trafficTime, @RequestParam Integer year) {
		return ResponseEntity.ok(GeoService
			.getPropertiesCollection(rideService.getIntersectionEdgesByOsmId(osmId, trafficTime, weekDay, year)));
	}

	@GetMapping("/intersection_edges/regionId/properties")
	public ResponseEntity<List<Map<String, Object>>> getIntersectionEdgePropertiesByRegionId(
			@RequestParam Long regionId, @RequestParam WeekDays weekDay, @RequestParam TrafficTimes trafficTime,
			@RequestParam Integer year) {
		return ResponseEntity.ok(GeoService
			.getPropertiesCollection(rideService.getIntersectionEdgesByRegionId(regionId, trafficTime, weekDay, year)));
	}

	@GetMapping("/intersection_edges/aggregate/complete")
	public ResponseEntity<Map<String, Object>> getIntersectionEdgesAggregateAsGeoJsonComplete(
			@RequestParam(required = false) Long numberOfRides, @RequestParam(required = false) WeekDays weekDay,
			@RequestParam(required = false) TrafficTimes trafficTime, @RequestParam(required = false) Integer year) {
		return ResponseEntity.ok(GeoService.getFeatureCollection(
				rideService.getIntersectionEdgeMetricsComplete(numberOfRides, weekDay, trafficTime, year)));
	}

	@GetMapping("/intersection_edges/aggregate")
	public ResponseEntity<Map<String, Object>> getIntersectionEdgesAsGeoJsonPageable(
			@RequestParam(required = false) Long osmId, @RequestParam(required = false) Long numberOfRides,
			@RequestParam(required = false) String region, @RequestParam(required = false) String name,
			@RequestParam(required = false) WeekDays weekDay, @RequestParam(required = false) TrafficTimes trafficTime,
			@RequestParam(required = false) Integer year, Pageable pageable) {
		return ResponseEntity.ok(rideService.getIntersectionEdgeMetricsPageable(osmId, numberOfRides, region, name,
				weekDay, trafficTime, year, pageable));
	}

	@GetMapping("/intersection_edges/streetNames")
	public List<String> getEdgeHighwayNames(@RequestParam(required = false) Long count,
			@RequestParam(required = false) String region, @RequestParam(required = false) String name) {
		return rideService.findAllStreetNamesIntersectionEdge(count, region, name);
	}

	@GetMapping("/ids")
	public ResponseEntity<Map<String, Object>> getRideIds(@RequestParam(required = false) Long id, Pageable pageable) {
		return ResponseEntity.ok(rideService.getRideIdsPageable(id, pageable));
	}

	@GetMapping("/ride/{intersectionBaseId}")
	public ResponseEntity<List<Long>> getRideIdByIntersectionBaseId(@PathVariable Long intersectionBaseId) {
		return ResponseEntity.ok(rideService.getRideIdByIntersectionBaseId(intersectionBaseId));
	}

	@GetMapping("/rideIds/{osmLineId}")
	public ResponseEntity<List<Long>> findByOsmLineId(@PathVariable Long osmLineId) {
		List<Long> rideIds = rideService.findByOsmLineId(osmLineId);
		return ResponseEntity.ok(rideIds);
	}

	@GetMapping("/regions/complete")
	public ResponseEntity<Map<String, Object>> getIntersectionRegionMetricsComplete(
			@RequestParam(required = false) Long numberOfRides, @RequestParam(required = false) WeekDays weekDay,
			@RequestParam(required = false) TrafficTimes trafficTime, @RequestParam(required = false) Integer year) {
		return ResponseEntity.ok(GeoService.getFeatureCollection(
				rideService.getIntersectionRegionMetricsComplete(numberOfRides, weekDay, trafficTime, year)));
	}

	@GetMapping("/regions/pageable")
	public ResponseEntity<Map<String, Object>> getIntersectionRegionMetricsPageable(
			@RequestParam(required = false) Long regionId, @RequestParam(required = false) Integer adminLevel,
			@RequestParam(required = false) Long numberOfRides, @RequestParam(required = false) WeekDays weekDay,
			@RequestParam(required = false) TrafficTimes trafficTime, @RequestParam(required = false) Integer year,
			Pageable pageable) {
		return ResponseEntity.ok(rideService.getIntersectionRegionMetricsPageable(regionId, adminLevel, numberOfRides,
				weekDay, trafficTime, year, pageable));
	}

	@GetMapping("/regions/rides")
	public ResponseEntity<List<Map<String, Object>>> getIntersectionRideRegionMetricsProperties(
			@RequestParam Long regionId, @RequestParam WeekDays weekDay, @RequestParam TrafficTimes trafficTime,
			@RequestParam Integer year) {
		return ResponseEntity.ok(GeoService.getPropertiesCollection(
				rideService.getIntersectionRideRegionMetrics(regionId, weekDay, trafficTime, year)));
	}

	@GetMapping(value = "/node-metrics/tiles/{z}/{x}/{y}.pbf", produces = "application/x-protobuf")
	public ResponseEntity<byte[]> getNodeMetricsTiles(@PathVariable int z, @PathVariable int x, @PathVariable int y,
			@RequestParam Long numberOfRides, @RequestParam String weekDay, @RequestParam String trafficTime,
			@RequestParam Integer year) {

		byte[] tile = rideService.getNodeMetricsTile(z, x, y, numberOfRides, weekDay, trafficTime, year);
		if (tile.length == 0) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.ok(tile);
	}

	@GetMapping(value = "/node-metrics/start/tiles/{z}/{x}/{y}.pbf", produces = "application/x-protobuf")
	public ResponseEntity<byte[]> getNodeMetricsStartTiles(@PathVariable int z, @PathVariable int x,
			@PathVariable int y, @RequestParam Long numberOfRides, @RequestParam String weekDay,
			@RequestParam String trafficTime, @RequestParam Integer year) {

		byte[] tile = rideService.getNodeMetricsStartTile(z, x, y, numberOfRides, weekDay, trafficTime, year);
		if (tile.length == 0) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.ok(tile);
	}

	@GetMapping(value = "/edge-metrics/tiles/{z}/{x}/{y}.pbf", produces = "application/x-protobuf")
	public ResponseEntity<byte[]> getEdgeMetricsTiles(@PathVariable int z, @PathVariable int x, @PathVariable int y,
			@RequestParam Long numberOfRides, @RequestParam String weekDay, @RequestParam String trafficTime,
			@RequestParam Integer year) {

		byte[] tile = rideService.getEdgeMetricsTile(z, x, y, numberOfRides, weekDay, trafficTime, year);
		if (tile.length == 0) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.ok(tile);
	}

	@GetMapping(value = "/edge-metrics/start/tiles/{z}/{x}/{y}.pbf", produces = "application/x-protobuf")
	public ResponseEntity<byte[]> getEdgeMetricsStartTiles(@PathVariable int z, @PathVariable int x,
			@PathVariable int y, @RequestParam Long numberOfRides, @RequestParam String weekDay,
			@RequestParam String trafficTime, @RequestParam Integer year) {

		byte[] tile = rideService.getEdgeMetricsStartTile(z, x, y, numberOfRides, weekDay, trafficTime, year);
		if (tile.length == 0) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.ok(tile);
	}

}
