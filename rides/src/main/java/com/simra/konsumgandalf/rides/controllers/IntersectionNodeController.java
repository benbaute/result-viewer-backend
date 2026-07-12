package com.simra.konsumgandalf.rides.controllers;

import com.simra.konsumgandalf.common.models.entities.IntersectionNode;
import com.simra.konsumgandalf.common.models.entities.IntersectionNodeMetrics;
import com.simra.konsumgandalf.common.models.enums.TrafficTimes;
import com.simra.konsumgandalf.common.models.enums.WeekDays;
import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;
import com.simra.konsumgandalf.rides.repositories.IntersectionNodeMetricsRepository;
import com.simra.konsumgandalf.rides.repositories.IntersectionNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/intersections/intersection_nodes")
@RequiredArgsConstructor
public class IntersectionNodeController {

	private final IntersectionNodeRepository intersectionNodeRepository;

	private final IntersectionNodeMetricsRepository intersectionNodeMetricsRepository;

	@GetMapping("/{rideId}")
	public ResponseEntity<Map<String, Object>> getIntersectionNodesAsGeoJson(@PathVariable Long rideId) {
		return ResponseEntity.ok(FeatureMappable.toFeatureCollection(intersectionNodeRepository.findByRideId(rideId)));
	}

	@GetMapping
	public ResponseEntity<Map<String, Object>> getIntersectionNodes(@RequestParam Boolean properties,
			@RequestParam(required = false) Long trafficSignalClusterId,
			@RequestParam(required = false) Long startValhallaEdgeId,
			@RequestParam(required = false) Long endValhallaEdgeId,
			@RequestParam(required = false) String regionLTreePath, @RequestParam(required = false) WeekDays weekDay,
			@RequestParam(required = false) TrafficTimes trafficTime, @RequestParam(required = false) Integer year,
			@RequestParam(required = false) Long startDate, @RequestParam(required = false) Long endDate,
			Pageable pageable) {
		Specification<IntersectionNode> spec = intersectionNodeRepository.createSpecification(trafficSignalClusterId,
				startValhallaEdgeId, endValhallaEdgeId, regionLTreePath, trafficTime, weekDay, year,
				startDate != null ? new Date(startDate) : null, endDate != null ? new Date(endDate) : null, false);
		return ResponseEntity.ok(intersectionNodeRepository.fetchPageCollection(spec, pageable, properties));
	}

	@GetMapping("/scroll")
	public ResponseEntity<Map<String, Object>> getIntersectionNodesScroll(
			@RequestParam(required = false) Long trafficSignalClusterId,
			@RequestParam(required = false) Long startValhallaEdgeId,
			@RequestParam(required = false) Long endValhallaEdgeId,
			@RequestParam(required = false) String regionLTreePath, @RequestParam(required = false) WeekDays weekDay,
			@RequestParam(required = false) TrafficTimes trafficTime, @RequestParam(required = false) Integer year,
			@RequestParam(required = false) Long startDate, @RequestParam(required = false) Long endDate,
			@RequestParam Integer pageSize, @RequestParam(required = false) String lastId) {
		Specification<IntersectionNode> spec = intersectionNodeRepository.createSpecification(trafficSignalClusterId,
				startValhallaEdgeId, endValhallaEdgeId, regionLTreePath, trafficTime, weekDay, year,
				startDate != null ? new Date(startDate) : null, endDate != null ? new Date(endDate) : null, false);
		return ResponseEntity.ok(intersectionNodeRepository.fetchScrollCollection(spec, pageSize, lastId));
	}

	@GetMapping("/count")
	public ResponseEntity<Long> getIntersectionNodesCount(@RequestParam(required = false) Long trafficSignalClusterId,
			@RequestParam(required = false) Long startValhallaEdgeId,
			@RequestParam(required = false) Long endValhallaEdgeId,
			@RequestParam(required = false) String regionLTreePath, @RequestParam(required = false) WeekDays weekDay,
			@RequestParam(required = false) TrafficTimes trafficTime, @RequestParam(required = false) Integer year,
			@RequestParam(required = false) Long startDate, @RequestParam(required = false) Long endDate) {
		Specification<IntersectionNode> spec = intersectionNodeRepository.createSpecification(trafficSignalClusterId,
				startValhallaEdgeId, endValhallaEdgeId, regionLTreePath, trafficTime, weekDay, year,
				startDate != null ? new Date(startDate) : null, endDate != null ? new Date(endDate) : null, true);
		return ResponseEntity.ok(intersectionNodeRepository.count(spec));
	}

	@GetMapping("/aggregate")
	public ResponseEntity<Map<String, Object>> getIntersectionNodesAggregateAsGeoJsonPageable(
			@RequestParam Boolean properties, @RequestParam(required = false) Long trafficSignalClusterId,
			@RequestParam(required = false) Long startValhallaEdgeId,
			@RequestParam(required = false) Long endValhallaEdgeId, @RequestParam(required = false) Long numberOfRides,
			@RequestParam(required = false) String regionLTreePath, @RequestParam(required = false) String streetNames,
			@RequestParam(required = false) WeekDays weekDay, @RequestParam(required = false) TrafficTimes trafficTime,
			@RequestParam(required = false) Integer year, Pageable pageable) {
		Specification<IntersectionNodeMetrics> spec = intersectionNodeMetricsRepository.createSpecification(
				trafficSignalClusterId, startValhallaEdgeId, endValhallaEdgeId, numberOfRides, regionLTreePath,
				streetNames, weekDay, trafficTime, year);
		return ResponseEntity.ok(intersectionNodeMetricsRepository.fetchPageCollection(spec, pageable, properties));
	}

	@GetMapping("/aggregate/scroll")
	public ResponseEntity<Map<String, Object>> getIntersectionNodesAggregateScroll(
			@RequestParam(required = false) Long trafficSignalClusterId,
			@RequestParam(required = false) Long startValhallaEdgeId,
			@RequestParam(required = false) Long endValhallaEdgeId, @RequestParam(required = false) Long numberOfRides,
			@RequestParam(required = false) String regionLTreePath, @RequestParam(required = false) String streetNames,
			@RequestParam(required = false) WeekDays weekDay, @RequestParam(required = false) TrafficTimes trafficTime,
			@RequestParam(required = false) Integer year, @RequestParam Integer pageSize,
			@RequestParam(required = false) String lastId) {
		Specification<IntersectionNodeMetrics> spec = intersectionNodeMetricsRepository.createSpecification(
				trafficSignalClusterId, startValhallaEdgeId, endValhallaEdgeId, numberOfRides, regionLTreePath,
				streetNames, weekDay, trafficTime, year);
		return ResponseEntity.ok(intersectionNodeMetricsRepository.fetchScrollCollection(spec, pageSize, lastId));
	}

	@GetMapping("/aggregate/count")
	public ResponseEntity<Long> getIntersectionNodesAggregateCount(
			@RequestParam(required = false) Long trafficSignalClusterId,
			@RequestParam(required = false) Long startValhallaEdgeId,
			@RequestParam(required = false) Long endValhallaEdgeId, @RequestParam(required = false) Long numberOfRides,
			@RequestParam(required = false) String regionLTreePath, @RequestParam(required = false) String streetNames,
			@RequestParam(required = false) WeekDays weekDay, @RequestParam(required = false) TrafficTimes trafficTime,
			@RequestParam(required = false) Integer year) {
		Specification<IntersectionNodeMetrics> spec = intersectionNodeMetricsRepository.createSpecification(
				trafficSignalClusterId, startValhallaEdgeId, endValhallaEdgeId, numberOfRides, regionLTreePath,
				streetNames, weekDay, trafficTime, year);
		return ResponseEntity.ok(intersectionNodeMetricsRepository.count(spec));
	}

	@GetMapping("/streetNames")
	public List<String> getHighwayNames(@RequestParam(required = false) String regionLTreePath,
			@RequestParam(required = false) String streetNames) {
		return intersectionNodeRepository.findAllIncludingString(regionLTreePath, streetNames);
	}

	@GetMapping(value = "/node-metrics/tiles/{z}/{x}/{y}.pbf", produces = "application/x-protobuf")
	public ResponseEntity<byte[]> getNodeMetricsTiles(@PathVariable int z, @PathVariable int x, @PathVariable int y,
			@RequestParam Long numberOfRides, @RequestParam String weekDay, @RequestParam String trafficTime,
			@RequestParam Integer year) {

		byte[] tile = intersectionNodeMetricsRepository.getNodeMetricsTile(z, x, y, numberOfRides, weekDay, trafficTime,
				year);
		if (tile.length == 0) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.ok(tile);
	}

	@GetMapping(value = "/node-metrics/start/tiles/{z}/{x}/{y}.pbf", produces = "application/x-protobuf")
	public ResponseEntity<byte[]> getNodeMetricsStartTiles(@PathVariable int z, @PathVariable int x,
			@PathVariable int y, @RequestParam Long numberOfRides, @RequestParam String weekDay,
			@RequestParam String trafficTime, @RequestParam Integer year) {

		byte[] tile = intersectionNodeMetricsRepository.getNodeMetricsStartTile(z, x, y, numberOfRides, weekDay,
				trafficTime, year);
		if (tile.length == 0) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.ok(tile);
	}

}
